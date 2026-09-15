package coffee.api.services.services_interface.shop;

import coffee.api.dto.request.shop.EditShopRequest;

public interface IEditShopService {
  void process(EditShopRequest request);
}
