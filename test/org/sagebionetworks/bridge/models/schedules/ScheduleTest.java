package org.sagebionetworks.bridge.models.schedules;

import static org.junit.Assert.assertEquals;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.junit.Test;
import org.sagebionetworks.bridge.json.BridgeObjectMapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;

public class ScheduleTest {

    @Test
    public void equalsHashCode() {
        EqualsVerifier.forClass(Schedule.class).suppress(Warning.NONFINAL_FIELDS).allFieldsShouldBeUsed().verify();
    }
    
    @SuppressWarnings("deprecation")
    @Test
    public void canRountripSerialize() throws Exception {
        Activity activity = new Activity.Builder().withLabel("label").withTask("ref").build();
        
        Schedule schedule = new Schedule();
        schedule.getActivities().add(activity);
        schedule.setCronTrigger("0 0 8 ? * TUE *");
        schedule.setDelay(Period.parse("P1D"));
        schedule.setExpires(Period.parse("P2D"));
        schedule.setStartsOn(DateTime.parse("2015-02-02T10:10:10.000Z"));
        schedule.setEndsOn(DateTime.parse("2015-01-01T10:10:10.000Z"));
        schedule.setEventId("eventId");
        schedule.setInterval(Period.parse("P3D"));
        schedule.setLabel("label");
        schedule.setScheduleType(ScheduleType.RECURRING);
        schedule.setTimes(Lists.newArrayList(LocalTime.parse("10:10"), LocalTime.parse("14:00")));
        
        BridgeObjectMapper mapper = BridgeObjectMapper.get();
        String string = mapper.writeValueAsString(schedule);

        JsonNode node = mapper.readTree(string);
        assertEquals("label", node.get("label").asText());
        assertEquals("recurring", node.get("scheduleType").asText());
        assertEquals("eventId", node.get("eventId").asText());
        assertEquals("0 0 8 ? * TUE *", node.get("cronTrigger").asText());
        assertEquals("P1D", node.get("delay").asText());
        assertEquals("P3D", node.get("interval").asText());
        assertEquals("P2D", node.get("expires").asText());
        assertEquals("2015-02-02T10:10:10.000Z", node.get("startsOn").asText());
        assertEquals("2015-01-01T10:10:10.000Z", node.get("endsOn").asText());
        assertEquals("Schedule", node.get("type").asText());

        ArrayNode times = (ArrayNode)node.get("times");
        assertEquals("10:10:00.000", times.get(0).asText());
        assertEquals("14:00:00.000", times.get(1).asText());
        
        JsonNode actNode = node.get("activities").get(0);
        assertEquals("label", actNode.get("label").asText());
        assertEquals("task", actNode.get("activityType").asText());
        assertEquals("ref", actNode.get("ref").asText());
        assertEquals("Activity", actNode.get("type").asText());

        JsonNode taskNode = actNode.get("task");
        assertEquals("ref", taskNode.get("identifier").asText());
        assertEquals("TaskReference", taskNode.get("type").asText());
        
        schedule = mapper.readValue(string, Schedule.class);
        assertEquals("0 0 8 ? * TUE *", schedule.getCronTrigger());
        assertEquals("P1D", schedule.getDelay().toString());
        assertEquals("P2D", schedule.getExpires().toString());
        assertEquals("eventId", schedule.getEventId());
        assertEquals("label", schedule.getLabel());
        assertEquals("P3D", schedule.getInterval().toString());
        assertEquals(ScheduleType.RECURRING, schedule.getScheduleType());
        assertEquals("2015-02-02T10:10:10.000Z", schedule.getStartsOn().toString());
        assertEquals("2015-01-01T10:10:10.000Z", schedule.getEndsOn().toString());
        assertEquals("10:10:00.000", schedule.getTimes().get(0).toString());
        assertEquals("14:00:00.000", schedule.getTimes().get(1).toString());
        activity = schedule.getActivities().get(0);
        assertEquals("label", activity.getLabel());
        assertEquals("ref", activity.getRef());
        assertEquals("ref", activity.getTask().getIdentifier());
    }
    
    @Test
    public void testStringSetters() {
        DateTime date = DateTime.parse("2015-02-02T10:10:10.000Z");
        Period period = Period.parse("P1D");
        Schedule schedule = new Schedule();
        schedule.setDelay("P1D");
        schedule.setEndsOn("2015-02-02T10:10:10.000Z");
        schedule.setStartsOn("2015-02-02T10:10:10.000Z");
        schedule.setExpires("P1D");
        schedule.setInterval("P1D");
        schedule.addTimes("10:10");
        schedule.addTimes("12:10");
        
        assertEquals(period, schedule.getDelay());
        assertEquals(date, schedule.getEndsOn());
        assertEquals(date, schedule.getStartsOn());
        assertEquals(period, schedule.getExpires());
        assertEquals(period, schedule.getInterval());
        assertEquals(Lists.newArrayList(LocalTime.parse("10:10"), LocalTime.parse("12:10")), schedule.getTimes());
    }
}
