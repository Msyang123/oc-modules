package com.lhiot.oc.basic.model;

import com.lhiot.oc.basic.model.type.AvailableStatus;
import com.lhiot.oc.basic.model.type.InventorySpecification;
import com.lhiot.oc.basic.model.type.ProductAttachment;
import com.lhiot.oc.basic.model.type.ShelfStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author zhangfeng created in 2018/9/21 16:22
 **/
@Data
@ApiModel
public class ProductShelfResult {
    private Long shelfId;
    private String shelfName;
    private BigDecimal specificationQty;
    private ShelfStatus shelfStatus;
    private String shelfImage;
    private Integer originalPrice;
    private String description;
    @ApiModelProperty(notes = "规格Id", dataType = "Long")
    private Long specificationId;
    @ApiModelProperty(notes = "规格条码", dataType = "String")
    private String barcode;
    @ApiModelProperty(notes = "打包单位", dataType = "String")
    private String packagingUnit;
    @ApiModelProperty(notes = "单个规格重量", dataType = "BigDecimal")
    private BigDecimal weight;
    @ApiModelProperty(notes = "安全库存", dataType = "Long")
    private Long limitInventory;
    @ApiModelProperty(notes = "售价", dataType = "Long")
    private Long price;
    @ApiModelProperty(notes = "是否是库存规格：YES-是，NO-否", dataType = "InventorySpecification")
    private InventorySpecification inventorySpecification;
    @ApiModelProperty(notes = "规格是否可用：YES-是，NO-否", dataType = "AvailableStatus")
    private AvailableStatus availableStatus;
    @ApiModelProperty(notes = "商品Id", dataType = "Long")
    private Long productId;
    @ApiModelProperty(notes = "商品名称", dataType = "String")
    private String productName;
    @ApiModelProperty(notes = "商品主图", dataType = "String")
    private String productImage;
    private List<ProductAttachment> attachmentList;
}
