package coffee.api.services.services_implement.drink;

import coffee.api.dto.request.drink.CreateDrinksRequest;
import coffee.api.mapper.CreateDrinkMapper;
import coffee.api.services.services_interface.drink.ICreateDrinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateDrinkServiceImpl implements ICreateDrinkService {

  private final CreateDrinkMapper createDrinksMapper;

  @Override
  @Transactional
  public void process(CreateDrinksRequest request) {

  }
}