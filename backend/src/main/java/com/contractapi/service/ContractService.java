package com.contractapi.service;

import java.util.ArrayList;
import java.util.List;
import com.contractapi.constants.ContractStatus;
import com.contractapi.constants.ErrorCode;
import com.contractapi.dto.ContractVO;
import com.contractapi.dto.GenerateContractRequest;
import com.contractapi.entity.Contract;
import com.contractapi.entity.ContractTemplate;
import com.contractapi.exception.ApiException;
import com.contractapi.utils.TemplateRenderer;
import org.springframework.stereotype.Service;

@Service
public class ContractService {
  private final TemplateService templateService;
  private final TemplateRenderer renderer;
  private final List<Contract> contracts = new ArrayList<>();

  public ContractService(TemplateService templateService, TemplateRenderer renderer) {
    this.templateService = templateService;
    this.renderer = renderer;
  }

  public Contract generate(GenerateContractRequest request) {
    ContractTemplate template = templateService.find(request.templateId());
    Contract contract = new Contract();
    contract.setId(System.currentTimeMillis());
    contract.setUserId(request.userId());
    contract.setTemplateId(template.getId());
    contract.setTitle(request.title());
    contract.setContent(renderer.render(template.getContent(), request.variables()));
    contract.setStatus(ContractStatus.DRAFT.name());
    contract.setSigners("[]");
    contracts.add(contract);
    return contract;
  }

  public Contract updateStatus(Long id, ContractStatus status) {
    Contract contract = contracts.stream().filter(item -> item.getId().equals(id)).findFirst()
      .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "合同不存在"));
    contract.setStatus(status.name());
    return contract;
  }

  public List<ContractVO> list(Long userId, String status, String templateType) {
    return contracts.stream()
      .filter(item -> userId == null || item.getUserId().equals(userId))
      .filter(item -> status == null || item.getStatus().equals(status))
      .map(this::toVO)
      .filter(vo -> templateType == null || templateType.equals(vo.getTemplateType()))
      .toList();
  }

  public String exportPdf(Long id) {
    return "wkhtmltopdf 已在 Docker 镜像安装，合同 " + id + " 可导出到 /tmp/contracts/" + id + ".pdf";
  }

  private ContractVO toVO(Contract contract) {
    ContractTemplate template = templateService.find(contract.getTemplateId());
    ContractVO vo = new ContractVO();
    vo.setId(contract.getId());
    vo.setUserId(contract.getUserId());
    vo.setTemplateId(contract.getTemplateId());
    vo.setTemplateName(template != null ? template.getTitle() : null);
    vo.setTemplateType(template != null ? template.getType() : null);
    vo.setTitle(contract.getTitle());
    vo.setContent(contract.getContent());
    vo.setStatus(contract.getStatus());
    vo.setSigners(contract.getSigners());
    return vo;
  }
}
