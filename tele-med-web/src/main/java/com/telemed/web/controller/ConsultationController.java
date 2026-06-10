package com.telemed.web.controller;

import com.telemed.common.dto.ConsultationAcceptDTO;
import com.telemed.common.dto.ConsultationCreateDTO;
import com.telemed.common.result.Result;
import com.telemed.common.vo.ConsultationVO;
import com.telemed.model.entity.ChatMessage;
import com.telemed.model.entity.Consultation;
import com.telemed.service.ChatService;
import com.telemed.service.ConsultationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/consultation")
public class ConsultationController {

    @Autowired
    private ConsultationService consultationService;

    @Autowired
    private ChatService chatService;

    @PostMapping("/create")
    public Result<Consultation> create(@RequestBody ConsultationCreateDTO dto) {
        Consultation consultation = consultationService.createConsultation(
                dto.getPatientId(), dto.getDoctorId(), dto.getType());
        return Result.ok(consultation);
    }

    @PostMapping("/accept")
    public Result<Consultation> accept(@RequestBody ConsultationAcceptDTO dto) {
        Consultation consultation = consultationService.acceptConsultation(
                dto.getConsultationId(), dto.getDoctorId());
        return Result.ok(consultation);
    }

    @PostMapping("/finish")
    public Result<Consultation> finish(@RequestParam Long consultationId,
                                       @RequestParam(required = false) String conclusionContent) {
        Consultation consultation = consultationService.finishConsultation(consultationId, conclusionContent);
        return Result.ok(consultation);
    }

    @GetMapping("/waiting")
    public Result<List<ConsultationVO>> waiting() {
        List<ConsultationVO> list = consultationService.getWaitingConsultationVOList();
        return Result.ok(list);
    }

    @GetMapping("/doctor/{doctorId}")
    public Result<List<ConsultationVO>> doctorConsultations(@PathVariable Long doctorId,
                                                             @RequestParam(required = false) Integer status) {
        List<ConsultationVO> list = consultationService.getDoctorConsultationVOList(doctorId, status);
        return Result.ok(list);
    }

    @GetMapping("/patient/{patientId}")
    public Result<List<ConsultationVO>> patientConsultations(@PathVariable Long patientId) {
        List<ConsultationVO> list = consultationService.getPatientConsultationVOList(patientId);
        return Result.ok(list);
    }

    @GetMapping("/detail/{consultationNo}")
    public Result<Consultation> detail(@PathVariable String consultationNo) {
        Consultation consultation = consultationService.getByConsultationNo(consultationNo);
        return Result.ok(consultation);
    }

    @GetMapping("/{id}")
    public Result<ConsultationVO> getById(@PathVariable Long id) {
        ConsultationVO vo = consultationService.getConsultationVOById(id);
        return Result.ok(vo);
    }

    @GetMapping("/chat/{consultationId}")
    public Result<List<ChatMessage>> chatMessages(@PathVariable Long consultationId) {
        List<ChatMessage> list = chatService.getConsultationMessages(consultationId);
        return Result.ok(list);
    }
}
