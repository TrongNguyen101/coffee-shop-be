package coffee.api.services.services_interface.shop;

import coffee.api.dto.request.shop.CreateShopRequest;

public interface ICreateShopService {
  void process(CreateShopRequest request);
}
