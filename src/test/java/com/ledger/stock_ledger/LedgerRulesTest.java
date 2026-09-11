package com.ledger.stock_ledger;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(EmbeddedPostgresConfiguration.class)
class LedgerRulesTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void currentAndAsOfStockMatchAFixedTimeline() throws Exception {
        UUID item = createItem("PEN-" + suffix(), "Blue Pen");
        UUID north = createWarehouse("WH-N-" + suffix(), "North");
        UUID south = createWarehouse("WH-S-" + suffix(), "South");

        postMovement("""
                {
                  "kind": "IN",
                  "itemId": "%s",
                  "quantity": 100,
                  "warehouseId": "%s",
                  "reason": "opening stock",
                  "occurredAt": "2026-03-01T09:00:00Z",
                  "recordedBy": "alex"
                }
                """.formatted(item, north));

        UUID outId = postMovement("""
                {
                  "kind": "OUT",
                  "itemId": "%s",
                  "quantity": 15,
                  "warehouseId": "%s",
                  "reason": "customer order 12",
                  "occurredAt": "2026-03-02T12:00:00Z",
                  "recordedBy": "alex"
                }
                """.formatted(item, north));

        postMovement("""
                {
                  "kind": "TRANSFER",
                  "itemId": "%s",
                  "quantity": 20,
                  "fromWarehouseId": "%s",
                  "toWarehouseId": "%s",
                  "reason": "restock south",
                  "occurredAt": "2026-03-04T08:00:00Z",
                  "recordedBy": "sam"
                }
                """.formatted(item, north, south));

        postMovement("""
                {
                  "kind": "IN",
                  "itemId": "%s",
                  "quantity": 10,
                  "warehouseId": "%s",
                  "reason": "purchase order 4471",
                  "occurredAt": "2026-03-10T10:00:00Z",
                  "recordedBy": "alex"
                }
                """.formatted(item, north));

        assertThat(stockTotal("/api/stock/as-of?itemId=" + item + "&warehouseId=" + north + "&at=2026-03-03T00:00:00Z")).isEqualTo(85);
        assertThat(stockTotal("/api/stock/as-of?itemId=" + item + "&at=2026-03-03T00:00:00Z")).isEqualTo(85);
        assertThat(stockTotal("/api/stock?itemId=" + item + "&warehouseId=" + north)).isEqualTo(75);
        assertThat(stockTotal("/api/stock?itemId=" + item)).isEqualTo(95);

        mockMvc.perform(post("/api/movements/" + outId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recordedBy": "alex", "reason": "wrong quantity"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kind").value("CANCEL"))
                .andExpect(jsonPath("$.cancelsMovementId").value(outId.toString()));

        assertThat(stockTotal("/api/stock?itemId=" + item + "&warehouseId=" + north)).isEqualTo(90);
        assertThat(stockTotal("/api/stock/as-of?itemId=" + item + "&warehouseId=" + north + "&at=2026-03-03T00:00:00Z")).isEqualTo(85);
    }

    @Test
    void cancelLeavesBothRowsAndCannotBeRepeated() throws Exception {
        UUID item = createItem("INK-" + suffix(), "Black Ink");
        UUID warehouse = createWarehouse("WH-" + suffix(), "Main");

        UUID original = postMovement("""
                {
                  "kind": "IN",
                  "itemId": "%s",
                  "quantity": 8,
                  "warehouseId": "%s",
                  "reason": "delivery",
                  "occurredAt": "2026-04-01T09:00:00Z",
                  "recordedBy": "pat"
                }
                """.formatted(item, warehouse));

        MvcResult cancelResult = mockMvc.perform(post("/api/movements/" + original + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recordedBy": "pat", "reason": "counted twice"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        UUID cancelId = idFrom(cancelResult);

        mockMvc.perform(get("/api/movements/" + original))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("IN"))
                .andExpect(jsonPath("$.quantity").value(8))
                .andExpect(jsonPath("$.cancelledByMovementId").value(cancelId.toString()));

        mockMvc.perform(get("/api/movements/" + cancelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("CANCEL"))
                .andExpect(jsonPath("$.cancelsMovementId").value(original.toString()));

        mockMvc.perform(get("/api/items/" + item + "/movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].kind").value("CANCEL"))
                .andExpect(jsonPath("$.content[0].cancelsMovementId").value(original.toString()))
                .andExpect(jsonPath("$.content[1].kind").value("IN"))
                .andExpect(jsonPath("$.content[1].cancelledByMovementId").value(cancelId.toString()));

        mockMvc.perform(post("/api/movements/" + original + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recordedBy": "pat", "reason": "try again"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void renamingAnItemDoesNotChangeNamesOnOldMovements() throws Exception {
        UUID item = createItem("GEL-" + suffix(), "Blue Pen");
        UUID warehouse = createWarehouse("WH-" + suffix(), "Main");

        UUID movementId = postMovement("""
                {
                  "kind": "IN",
                  "itemId": "%s",
                  "quantity": 5,
                  "warehouseId": "%s",
                  "reason": "first delivery",
                  "occurredAt": "2026-05-01T09:00:00Z",
                  "recordedBy": "lee"
                }
                """.formatted(item, warehouse));

        mockMvc.perform(patch("/api/items/" + item)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Blue Gel Pen"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Blue Gel Pen"));

        mockMvc.perform(get("/api/movements/" + movementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemName").value("Blue Pen"))
                .andExpect(jsonPath("$.itemId").value(item.toString()));

        mockMvc.perform(get("/api/items/" + item))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Blue Gel Pen"));
    }

    @Test
    void backdatedArrivalChangesStockOnTheFollowingDay() throws Exception {
        UUID item = createItem("BOX-" + suffix(), "Box");
        UUID warehouse = createWarehouse("WH-" + suffix(), "Main");

        postMovement("""
                {
                  "kind": "IN",
                  "itemId": "%s",
                  "quantity": 10,
                  "warehouseId": "%s",
                  "reason": "arrived last Tuesday",
                  "occurredAt": "2026-06-02T10:00:00Z",
                  "recordedBy": "alex"
                }
                """.formatted(item, warehouse));

        assertThat(stockTotal("/api/stock/as-of?itemId=" + item + "&warehouseId=" + warehouse + "&at=2026-06-03T12:00:00Z")).isEqualTo(10);
    }

    @Test
    void refusesOutThatWouldGoNegativeAndDisabledItemMovements() throws Exception {
        UUID item = createItem("CUP-" + suffix(), "Cup");
        UUID warehouse = createWarehouse("WH-" + suffix(), "Main");

        postMovement("""
                {
                  "kind": "IN",
                  "itemId": "%s",
                  "quantity": 2,
                  "warehouseId": "%s",
                  "reason": "small delivery",
                  "occurredAt": "2026-07-01T09:00:00Z",
                  "recordedBy": "alex"
                }
                """.formatted(item, warehouse));

        mockMvc.perform(post("/api/movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "kind": "OUT",
                                  "itemId": "%s",
                                  "quantity": 5,
                                  "warehouseId": "%s",
                                  "reason": "too many",
                                  "occurredAt": "2026-07-02T09:00:00Z",
                                  "recordedBy": "alex"
                                }
                                """.formatted(item, warehouse)))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/items/" + item + "/disable"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "kind": "IN",
                                  "itemId": "%s",
                                  "quantity": 1,
                                  "warehouseId": "%s",
                                  "reason": "after disable",
                                  "occurredAt": "2026-07-03T09:00:00Z",
                                  "recordedBy": "alex"
                                }
                                """.formatted(item, warehouse)))
                .andExpect(status().isConflict());

        assertThat(stockTotal("/api/stock?itemId=" + item + "&warehouseId=" + warehouse)).isEqualTo(2);
    }

    @Test
    void seedCreatesItemsAndWarehouses() throws Exception {
        mockMvc.perform(post("/api/demo/seed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].code").value("PEN-BLUE"))
                .andExpect(jsonPath("$.warehouses[0].code").value("WH-NORTH"));
    }

    private UUID createItem(String code, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "%s", "name": "%s", "unit": "PIECES"}
                                """.formatted(code, name)))
                .andExpect(status().isCreated())
                .andReturn();
        return idFrom(result);
    }

    private UUID createWarehouse(String code, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "%s", "name": "%s"}
                                """.formatted(code, name)))
                .andExpect(status().isCreated())
                .andReturn();
        return idFrom(result);
    }

    private UUID postMovement(String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return idFrom(result);
    }

    private int stockTotal(String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path)).andExpect(status().isOk()).andReturn();
        Object total = JsonPath.read(result.getResponse().getContentAsString(), "$.total");
        return Integer.parseInt(total.toString());
    }

    private UUID idFrom(MvcResult result) throws Exception {
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return UUID.fromString(id);
    }

    private String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
