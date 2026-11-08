package com.valtheron.horizon.mobile

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class NeuralGraphContractTest {
    private fun wire(): JSONObject = JSONObject(
        """
        {
          "type":"brain.inspection.page",
          "schema_version":1,
          "brain_identity":"horizon-primary-brain",
          "state_revision":237,
          "inspection_epoch":"fixture-live-epoch",
          "data":{
            "section":"neural_units",
            "order":"activity",
            "total":8,
            "counts":{"neural_units":8,"synapses":16},
            "items":[
              {"id":5,"representation":[0.3,-0.2],"activation":0.75,"importance":0.5,"plasticity":0.2},
              {"id":7,"representation":[0.4,0.6],"activation":0.12,"importance":0.7,"plasticity":0.4}
            ],
            "related_total":1,
            "related_synapses":[
              {"source_id":5,"target_id":7,"weight":0.63,"confidence":0.9,"inhibitory":false}
            ]
          }
        }
        """.trimIndent()
    )

    @Test fun singlePageIncludesActualEdgesAndFullCounts() {
        val page = wire()
        val graph = inspectionPageToGraph(page)
        assertEquals("BRIDGE · REAL", graph.getString("mode"))
        assertEquals(237L, graph.getLong("state_revision"))
        assertEquals("fixture-live-epoch", graph.getString("inspection_epoch"))
        assertEquals(2, graph.getJSONArray("nodes").length())
        assertEquals(1, graph.getJSONArray("edges").length())
        assertEquals(8, graph.getJSONObject("counts").getInt("neural_units"))
        assertEquals(16, graph.getJSONObject("counts").getInt("synapses"))
        val edge = graph.getJSONArray("edges").getJSONObject(0)
        assertEquals("5", edge.getString("s"))
        assertEquals("7", edge.getString("t"))
        assertEquals(0.63, edge.getDouble("w"), 1e-10)
        assertEquals(false, edge.getBoolean("inhibitory"))
    }

    @Test fun disconnectedPageRemainsRealDataWithoutFakeConnections() {
        val page = wire()
        page.getJSONObject("data").remove("related_synapses")
        val graph = inspectionPageToGraph(page)
        assertEquals(2, graph.getJSONArray("nodes").length())
        assertEquals(0, graph.getJSONArray("edges").length())
        assertEquals(16, graph.getJSONObject("counts").getInt("synapses"))
    }

    @Test fun foreignIdentityFailsClosed() {
        val page = wire()
        page.put("brain_identity", "foreign-brain")
        try {
            inspectionPageToGraph(page)
            fail("Foreign neural data must not be displayed as Horizon")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("Identitas"))
        }
    }

    @Test fun rejectsWrongSchemaAndSection() {
        val page = wire()
        page.put("schema_version", 2)
        try {
            inspectionPageToGraph(page)
            fail("schema 2 is not compatible")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("skema"))
        }
        page.put("schema_version", 1)
        page.getJSONObject("data").put("section", "synapses")
        try {
            inspectionPageToGraph(page)
            fail("synapse page must not be parsed as neural_units")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("neural_units"))
        }
    }

    @Test fun discardsEdgesWithInvisibleEndpoints() {
        val page = wire()
        page.getJSONObject("data").getJSONArray("related_synapses").getJSONObject(0)
            .put("target_id", 999)
        val graph = inspectionPageToGraph(page)
        assertEquals(0, graph.getJSONArray("edges").length())
    }
}
