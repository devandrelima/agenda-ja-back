package com.agendaja.backend.service;

import com.agendaja.backend.dto.DisponibilidadeRequestDTO;
import com.agendaja.backend.dto.DisponibilidadeResponseDTO;
import com.agendaja.backend.dto.FolgaDTO;
import com.agendaja.backend.dto.JornadaTrabalhoDTO;
import com.agendaja.backend.model.Folga;
import com.agendaja.backend.model.JornadaTrabalho;
import com.agendaja.backend.model.Profissional;
import com.agendaja.backend.repository.ProfissionalRepository;
import com.agendaja.backend.security.EstabelecimentoAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DisponibilidadeService {
    private final ProfissionalRepository profissionalRepository;
    private final EstabelecimentoAuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public DisponibilidadeResponseDTO consultar(Long estabelecimentoId, Long profissionalId, String emailGestor)
    {
        authorizationService.validarAcesso(emailGestor, estabelecimentoId);
        return DisponibilidadeResponseDTO.from(buscarProfissional(estabelecimentoId, profissionalId));
    }

    @Transactional
    public DisponibilidadeResponseDTO atualizar(Long estabelecimentoId, Long profissionalId, DisponibilidadeRequestDTO dto, String emailGestor)
    {
        authorizationService.validarAcesso(emailGestor, estabelecimentoId);
        
        var profissional = buscarProfissional(estabelecimentoId, profissionalId);

        validarJornadas(dto.jornadas());

        profissional.getJornadasTrabalho().clear();
        for (JornadaTrabalhoDTO j : dto.jornadas()) {
            var jornada = new JornadaTrabalho();
            jornada.setDiaSemana(j.diaSemana());
            jornada.setHoraInicio(j.horaInicio());
            jornada.setHoraFim(j.horaFim());
            jornada.setProfissional(profissional);
            profissional.getJornadasTrabalho().add(jornada);
        }

        profissional.getFolgas().clear();
        for (FolgaDTO f : dto.folgas()) {
            var folga = new Folga();
            folga.setData(f.data());
            folga.setMotivo(f.motivo());
            folga.setProfissional(profissional);
            profissional.getFolgas().add(folga);
        }

        return DisponibilidadeResponseDTO.from(profissional);
    }

    private void validarJornadas(List<JornadaTrabalhoDTO> jornadas)
    {
        for (var jornada : jornadas)
            if (!jornada.horaInicio().isBefore(jornada.horaFim()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A hora de início deve ser anterior à hora de fim.");

        for (int i = 0; i < jornadas.size(); i++) {
            for (int j = i + 1; j < jornadas.size(); j++) {
                var j1 = jornadas.get(i);
                var j2 = jornadas.get(j);

                if (j1.diaSemana().equals(j2.diaSemana())) {
                    boolean sobrepoe = j1.horaInicio().isBefore(j2.horaFim()) && j2.horaInicio().isBefore(j1.horaFim());
                    if (sobrepoe)
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Existem jornadas sobrepostas para o mesmo dia.");
                }
            }
        }
    }

    private Profissional buscarProfissional(Long estabelecimentoId, Long profissionalId)
    {
        return profissionalRepository.findByIdAndEstabelecimentoId(profissionalId, estabelecimentoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional não encontrado"));
    }
}
