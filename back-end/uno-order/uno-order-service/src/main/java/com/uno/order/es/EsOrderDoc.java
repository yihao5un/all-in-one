package com.uno.order.es;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "uno_order_index")
public class EsOrderDoc {

    @Id
    private Long id;

    @Field(type = FieldType.Keyword)
    private String orderNo;

    @Field(type = FieldType.Long)
    private Long employeeId;

    @Field(type = FieldType.Text)
    private String employeeName;

    @Field(type = FieldType.Keyword)
    private String orderType;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private String thirdSyncStatus;

    @Field(type = FieldType.Text)
    private String thirdSyncMsg;

    @Field(type = FieldType.Text)
    private String productNames;

    @Field(type = FieldType.Boolean)
    private Boolean billSyncSent;

    @Field(type = FieldType.Text)
    private String remark;

    @Field(type = FieldType.Keyword)
    private String createTime;
}
