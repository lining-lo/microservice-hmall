package com.heima.item;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.item.domain.dto.ItemDocDTO;
import com.heima.item.domain.po.Item;
import com.heima.item.service.IItemService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

@SpringBootTest(properties = "spring.profiles.active=local")
public class EsTest {

    private RestHighLevelClient client;

    @Autowired
    private IItemService itemService;

    /**
     * 完整 JSON 映射（JDK 11 不支持文本块，用字符串拼接）
     */
    private String ITEM_MAPPING = "{\n" +
            "  \"mappings\": {\n" +
            "    \"_doc\": {\n" +
            "      \"properties\": {\n" +
            "        \"id\": {\n" +
            "          \"type\": \"keyword\"\n" +
            "        },\n" +
            "        \"name\": {\n" +
            "          \"type\": \"text\",\n" +
            "          \"analyzer\": \"ik_smart\"\n" +
            "        },\n" +
            "        \"price\": {\n" +
            "          \"type\": \"integer\"\n" +
            "        },\n" +
            "        \"image\": {\n" +
            "          \"type\": \"keyword\",\n" +
            "          \"index\": false\n" +
            "        },\n" +
            "        \"category\": {\n" +
            "          \"type\": \"keyword\"\n" +
            "        },\n" +
            "        \"brand\": {\n" +
            "          \"type\": \"keyword\"\n" +
            "        },\n" +
            "        \"sold\": {\n" +
            "          \"type\": \"integer\"\n" +
            "        },\n" +
            "        \"commentCount\": {\n" +
            "          \"type\": \"integer\",\n" +
            "          \"index\": false\n" +
            "        },\n" +
            "        \"isAD\": {\n" +
            "          \"type\": \"boolean\"\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

    @BeforeEach
    void setUp() {
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("192.168.150.101:9200")
        ));
    }

    /**
     * 创建索引
     */
    @Test
    void testCreateIndex() throws IOException {
        // 如果索引已存在会报错，可先删除再创建（按需打开）
        // client.indices().delete(new DeleteIndexRequest("items"), RequestOptions.DEFAULT);

        CreateIndexRequest request = new CreateIndexRequest("items");
        request.source(ITEM_MAPPING, XContentType.JSON);
        client.indices().create(request, RequestOptions.DEFAULT);
    }

    /**
     * 批量导入商品到 ES
     */
    @Test
    void bulkItemsToES() throws IOException {
        int pageNum = 1;
        int pageSize = 1000;

        List<Item> records = itemService.lambdaQuery()
                .eq(Item::getStatus, 1)
                .page(Page.of(pageNum, pageSize))
                .getRecords();

        while (ObjectUtil.isNotEmpty(records)) {
            BulkRequest request = new BulkRequest();

            for (Item item : records) {
                ItemDocDTO doc = BeanUtil.toBean(item, ItemDocDTO.class);
                request.add(
                        new IndexRequest("items")
                                .id(item.getId().toString())
                                .source(JSONUtil.toJsonStr(doc), XContentType.JSON)
                );
            }

            client.bulk(request, RequestOptions.DEFAULT);

            // 下一页
            records = itemService.lambdaQuery()
                    .eq(Item::getStatus, 1)
                    .page(Page.of(++pageNum, pageSize))
                    .getRecords();
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        if (this.client != null) {
            this.client.close();
        }
    }
}