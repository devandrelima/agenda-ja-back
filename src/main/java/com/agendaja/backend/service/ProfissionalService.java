package com.agendaja.backend.service;

import com.agendaja.backend.dto.ProfissionalRequestDTO;
import com.agendaja.backend.dto.ProfissionalResponseDTO;
import com.agendaja.backend.model.Folga;
import com.agendaja.backend.model.JornadaTrabalho;
import com.agendaja.backend.model.Profissional;
import com.agendaja.backend.model.Servico;
import com.agendaja.backend.repository.EstabelecimentoRepository;
import com.agendaja.backend.repository.ProfissionalRepository;
import com.agendaja.backend.repository.ServicoRepository;
import com.agendaja.backend.security.EstabelecimentoAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfissionalService {
    private final ProfissionalRepository profissionalRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final ServicoRepository servicoRepository;
    private final EstabelecimentoAuthorizationService authorizationService;

    @Transactional
    public ProfissionalResponseDTO criar(Long estabelecimentoId, ProfissionalRequestDTO dto, String emailGestor)
    {
        authorizationService.validarAcesso(emailGestor, estabelecimentoId);

        var estabelecimento = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estabelecimento não encontrado"));

        var profissional = new Profissional();
        profissional.setNome(dto.nome());
        profissional.setEstabelecimento(estabelecimento);
        
        if (dto.diasDeAtendimento() != null) {
            for (Integer dia : dto.diasDeAtendimento()) {
                var jornada = new JornadaTrabalho();
                jornada.setDiaSemana(dia);
                jornada.setHoraInicio(LocalTime.of(8, 0));
                jornada.setHoraFim(LocalTime.of(18, 0));
                jornada.setProfissional(profissional);
                profissional.getJornadasTrabalho().add(jornada);
            }
        }
        
        if (dto.diasDeFolga() != null) {
            for (String dataStr : dto.diasDeFolga()) {
                var folga = new Folga();
                folga.setData(LocalDate.parse(dataStr));
                folga.setMotivo("Folga");
                folga.setProfissional(profissional);
                profissional.getFolgas().add(folga);
            }
        }
        
        validarEAssociarServicos(profissional, dto.idsServicos(), estabelecimentoId);

        return ProfissionalResponseDTO.from(profissionalRepository.save(profissional));
    }

    @Transactional(readOnly = true)
    public List<ProfissionalResponseDTO> listar(Long estabelecimentoId, String emailGestor)
    {
        authorizationService.validarAcesso(emailGestor, estabelecimentoId);
        return profissionalRepository.findByEstabelecimentoIdAndAtivoTrue(estabelecimentoId)
                .stream()
                .map(ProfissionalResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProfissionalResponseDTO consultar(Long estabelecimentoId, Long profissionalId, String emailGestor)
    {
        authorizationService.validarAcesso(emailGestor, estabelecimentoId);
        return ProfissionalResponseDTO.from(buscarProfissional(estabelecimentoId, profissionalId));
    }

    @Transactional
    public ProfissionalResponseDTO editar(Long estabelecimentoId, Long profissionalId, ProfissionalRequestDTO dto, String emailGestor)
    {
        authorizationService.validarAcesso(emailGestor, estabelecimentoId);

        var profissional = buscarProfissional(estabelecimentoId, profissionalId);
        profissional.setNome(dto.nome());
        
        if (dto.diasDeAtendimento() != null) {
            profissional.getJornadasTrabalho().removeIf(j -> !dto.diasDeAtendimento().contains(j.getDiaSemana()));
            
            List<Integer> diasExistentes = profissional.getJornadasTrabalho().stream().map(jornada -> jornada.getDiaSemana()).toList();
            for (Integer dia : dto.diasDeAtendimento()) {
                if (!diasExistentes.contains(dia)) {
                    var jornada = new JornadaTrabalho();
                    jornada.setDiaSemana(dia);
                    jornada.setHoraInicio(LocalTime.of(8, 0));
                    jornada.setHoraFim(LocalTime.of(18, 0));
                    jornada.setProfissional(profissional);
                    profissional.getJornadasTrabalho().add(jornada);
                }
            }
        }

        if (dto.diasDeFolga() != null) {
            List<LocalDate> novasFolgas = dto.diasDeFolga().stream().map(LocalDate::parse).toList();
            profissional.getFolgas().removeIf(f -> !novasFolgas.contains(f.getData()));
            
            List<LocalDate> folgasExistentes = profissional.getFolgas().stream().map(folga -> folga.getData()).toList();
            for (LocalDate novaFolga : novasFolgas) {
                if (!folgasExistentes.contains(novaFolga)) {
                    var folga = new Folga();
                    folga.setData(novaFolga);
                    folga.setMotivo("Folga");
                    folga.setProfissional(profissional);
                    profissional.getFolgas().add(folga);
                }
            }
        }
        
        validarEAssociarServicos(profissional, dto.idsServicos(), estabelecimentoId);

        return ProfissionalResponseDTO.from(profissional);
    }

    private Profissional buscarProfissional(Long estabelecimentoId, Long profissionalId)
    {
        return profissionalRepository.findByIdAndEstabelecimentoIdAndAtivoTrue(profissionalId, estabelecimentoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional não encontrado"));
    }
    
    private void validarEAssociarServicos(Profissional profissional, List<Long> idsServicos, Long estabelecimentoId)
    {
        List<Servico> servicos = servicoRepository.findAllById(idsServicos);
        
        boolean todosServicosDoEstabelecimento = servicos.stream()
                .allMatch(s -> s.getEstabelecimento().getId().equals(estabelecimentoId));
                
        if (servicos.size() != idsServicos.size() || !todosServicosDoEstabelecimento)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Um ou mais serviços são inválidos ou não pertencem ao estabelecimento.");
        
        profissional.setServicos(servicos);
    }

    @Transactional(readOnly = true)
    public List<ProfissionalResponseDTO> listarPorServicoPublico(Long estabelecimentoId, Long servicoId)
    {
        return profissionalRepository.findByEstabelecimentoIdAndServicosIdAndAtivoTrue(estabelecimentoId, servicoId)
                .stream()
                .map(ProfissionalResponseDTO::from)
                .toList();
    }

    @Transactional
    public void desativar(Long estabelecimentoId, Long profissionalId, String emailGestor)
    {
        authorizationService.validarAcesso(emailGestor, estabelecimentoId);
        var profissional = buscarProfissional(estabelecimentoId, profissionalId);
        profissional.setAtivo(false);
    }
}
