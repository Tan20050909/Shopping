package org.example.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.user.dto.ProductQueryDTO;
import org.example.user.vo.*;

import java.util.List;

@Mapper
public interface UserProductMapper {
    List<CategoryVO> selectEnabledCategories();

    Long countProducts(@Param("query") ProductQueryDTO query);

    List<ProductListItemVO> selectProducts(@Param("query") ProductQueryDTO query,
                                           @Param("offset") long offset,
                                           @Param("orderBy") String orderBy);

    Long countMerchantProducts(@Param("merchantId") Long merchantId, @Param("keyword") String keyword);

    List<ProductListItemVO> selectMerchantProducts(@Param("merchantId") Long merchantId,
                                                   @Param("keyword") String keyword,
                                                   @Param("offset") long offset,
                                                   @Param("pageSize") int pageSize);

    ProductDetailVO selectProductDetail(@Param("goodsId") Long goodsId);

    List<ProductSkuVO> selectProductSkus(@Param("goodsId") Long goodsId);

    List<ProductImageVO> selectProductImages(@Param("goodsId") Long goodsId);

    List<ProductCommentVO> selectProductComments(@Param("goodsId") Long goodsId);
}

