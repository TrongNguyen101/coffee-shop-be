package coffee.api.services.services_implement.shop;

import coffee.api.dto.request.shop.CreateShopRequest;
import coffee.api.exceptions.UserExistException;
import coffee.api.mapper.CreateShopMapper;
import coffee.api.services.services_interface.shop.ICreateShopService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateShopServiceImpl implements ICreateShopService {

  private final CreateShopMapper createShopMapper;

  @Override
  public void process(CreateShopRequest request) {
    Boolean isShopExisted = createShopMapper.checkShopExistedByName(request.getShopName().trim());
    if (Boolean.TRUE.equals(isShopExisted)) {
      throw new UserExistException("Shop name is existed");
    }

    Boolean isAddressExisted =
        createShopMapper.checkShopExistedByAddress(request.getAddress().trim());
    if (Boolean.TRUE.equals(isAddressExisted)) {
      throw new UserExistException("Address is existed");
    }

    String phoneNumber = request.getPhoneNumber().trim();

    UUID newShopId = UUID.randomUUID();
    Boolean isDeleted = false;

    createShopMapper.createShop(
        newShopId,
        request.getShopName().trim(),
        request.getAddress().trim(),
        phoneNumber,
        isDeleted);
  }
}
