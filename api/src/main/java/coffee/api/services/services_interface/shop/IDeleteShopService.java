package coffee.api.services.services_interface.shop;

import coffee.api.dto.request.shop.DeleteShopRequest;

public interface IDeleteShopService {

  void process(DeleteShopRequest request);
}
