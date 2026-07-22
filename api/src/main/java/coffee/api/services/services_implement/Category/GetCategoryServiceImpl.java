package coffee.api.services.services_implement.Category;

import coffee.api.dto.request.category.SearchCategoriesRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.response.base_response.PaginationMeta;
import coffee.api.dto.result.CategoryResult;
import coffee.api.repository.category.GetCategoryRepository;
import coffee.api.services.services_interface.Category.IGetCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetCategoryServiceImpl implements IGetCategoryService {

  private final GetCategoryRepository getCategoryRepository;

  @Override
  public PageResponse<CategoryResult> process(SearchCategoriesRequest request) {
    long totalElements = getCategoryRepository.countCategoriesFiltered(
      request.trimmedSearch(),
      null,
      null
    );

    List<CategoryResult> categories = getCategoryRepository.getCategoriesFiltered(
      request.trimmedSearch(),
      request.getSortBy(),
      request.getSortDirection().toString(),
      request.getSize(),
      request.calcOffset(),
      null,
      null
    );

    if (categories == null) {
      categories = Collections.emptyList();
    }

    PaginationMeta pagination = PaginationMeta.builder()
      .page(request.getPage())
      .size(request.getSize())
      .totalElements(totalElements)
      .totalPages(request.totalPages(totalElements))
      .build();

    return PageResponse.of("Get categories successfully", categories, pagination);
  }
}