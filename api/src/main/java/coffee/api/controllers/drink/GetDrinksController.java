package coffee.api.controllers.drink;

import coffee.api.dto.request.drink.SearchDrinksRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.response.drink.GetDrinkResponse;
import coffee.api.dto.result.DrinkResult;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.drink.IGetDrinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GetDrinksController {
  private final IGetDrinkService drinkService;

  @PostMapping("drinks")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
  public ResponseEntity<GetDrinkResponse> getDrinks(
    @AuthenticationPrincipal CustomUserDetail customUserDetail,
    @RequestBody @Valid SearchDrinksRequest request
  ) {
    PageResponse<DrinkResult> response = drinkService.process(
      request,
      customUserDetail.getShopId(),
      customUserDetail.getRoleName()
    );
    return ResponseEntity.ok()
      .body(GetDrinkResponse.of(
        ResponseCode.SUCCESS,
        "Drinks retrieved successfully",
        response
      ));
  }
}