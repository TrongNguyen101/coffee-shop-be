package coffee.api.controllers.shop;

import coffee.api.dto.request.shop.SearchShopRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.result.ShopResult;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.shop.IGetShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GetShopController {

  private final IGetShopService getShopService;

  @PostMapping("shops")
  @PreAuthorize("hasAnyRole('OWNER')")
  public PageResponse<ShopResult> shopBranchResult(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid SearchShopRequest request) {

    return getShopService.process(
        request, customUserDetail.getRoleName(), customUserDetail.getShopId());
  }
}
