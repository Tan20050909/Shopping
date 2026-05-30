package org.example.user.controller;

import org.example.common.PageResult;
import org.example.common.Result;
import org.example.user.dto.ProductQueryDTO;
import org.example.user.service.UserProductService;
import org.example.user.vo.CategoryVO;
import org.example.user.vo.ProductDetailVO;
import org.example.user.vo.ProductListItemVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserProductController {
    private final UserProductService userProductService;

    public UserProductController(UserProductService userProductService) {
        this.userProductService = userProductService;
    }

    @GetMapping("/categories")
    public Result<List<CategoryVO>> categories() {
        return Result.ok(userProductService.listCategories());
    }

    @GetMapping("/products")
    public Result<PageResult<ProductListItemVO>> products(@ModelAttribute ProductQueryDTO query) {
        return Result.ok(userProductService.listProducts(query));
    }

    @GetMapping("/products/{goodsId}")
    public Result<ProductDetailVO> productDetail(@PathVariable Long goodsId) {
        return Result.ok(userProductService.getProductDetail(goodsId));
    }

    @GetMapping("/shops/{merchantId}")
    public Result<java.util.Map<String, Object>> shop(@PathVariable Long merchantId) {
        return Result.ok(userProductService.getMerchantProfile(merchantId));
    }

    @GetMapping("/shops/{merchantId}/products")
    public Result<PageResult<ProductListItemVO>> shopProducts(@PathVariable Long merchantId,
                                                              @RequestParam(required = false) String keyword,
                                                              @RequestParam(defaultValue = "1") int pageNum,
                                                              @RequestParam(defaultValue = "12") int pageSize) {
        return Result.ok(userProductService.listMerchantProducts(merchantId, keyword, pageNum, pageSize));
    }
}
