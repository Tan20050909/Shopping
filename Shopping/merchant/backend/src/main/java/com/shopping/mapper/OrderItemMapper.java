package com.shopping.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopping.entity.OrderItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    @Select({
            "<script>",
            "select oi.goods_id as goodsId, sum(oi.num) as buyCount",
            "from tb_order_item oi",
            "join tb_order o on o.order_id = oi.order_id",
            "where oi.goods_id in",
            "<foreach collection='goodsIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "and (o.pay_status = 1 or o.pay_time is not null)",
            "group by oi.goods_id",
            "</script>"
    })
    List<Map<String, Object>> sumBuyCountByGoodsIds(@Param("goodsIds") List<Long> goodsIds);
}
