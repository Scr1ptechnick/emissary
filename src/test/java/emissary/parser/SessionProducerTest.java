package emissary.parser;

import emissary.core.IBaseDataObject;
import emissary.test.core.junit5.UnitTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SuppressWarnings("deprecation")
// TODO: either remove these tests or test the new Parser
class SessionProducerTest extends UnitTest {
    @Test
    void testBasicSetup() throws Exception {
        SimpleParser parser = new SimpleParser("This is a test".getBytes());
        SessionProducer sp = new SessionProducer(parser, "UNKNOWN");
        IBaseDataObject payload = sp.getNextSession("name");
        assertEquals("This is a test", new String(payload.data()), "Parser/Producer should create payload object with correct data");
    }

    @Test
    void testZoneAssignments() {
        SimpleParser parser = new SimpleParser("This is a test".getBytes());
        SessionProducer sp = new SessionProducer(parser, "UNKNOWN");
        DecomposedSession d = new DecomposedSession();
        d.setHeader("The Header".getBytes());
        d.setFooter("The Footer".getBytes());
        d.setData("The Data".getBytes());
        d.setClassification("Purple Nurple");
        d.addMetaData("foo", "bar");
        IBaseDataObject payload = sp.createAndLoadDataObject(d, "name");
        assertEquals("The Header", new String(payload.header()), "Incorrect header");
        assertEquals("The Footer", new String(payload.footer()), "Incorrect footer");
        assertEquals("The Data", new String(payload.data()), "Incorrect data");
        assertEquals("Purple Nurple", payload.getClassification(), "Incorrect classification");
        assertEquals("bar", payload.getStringParameter("foo"), "Incorrect metadata");
        assertEquals("name", payload.shortName(), "Incorrect name");
    }

    @Test
    void testAlternateViewAssignment() {
        SimpleParser parser = new SimpleParser("This is a test".getBytes());
        SessionProducer sp = new SessionProducer(parser, "UNKNOWN");
        DecomposedSession d = new DecomposedSession();
        d.addMetaData("FOO", "BAR");
        d.addMetaData("ALT_VIEW_FOOVIEW", "This is the view data".getBytes());
        d.addMetaData("ALT_VIEW_BARVIEW", "This is string data for a view");
        IBaseDataObject payload = sp.createAndLoadDataObject(d, "name");
        assertNotNull(payload.getAlternateView("FOOVIEW"), "Alt view must be created from byte array");
        assertNotNull(payload.getAlternateView("BARVIEW"), "Alt veiw must be created from string");
        assertEquals("This is the view data", new String(payload.getAlternateView("FOOVIEW")), "Byte array view data incorrect");
        assertEquals("This is string data for a view", new String(payload.getAlternateView("BARVIEW")), "String view data incorrect");
        assertNull(payload.getParameter("ALT_VIEW_FOOVIEW"), "View data must be removed from normal metadata");
        assertNull(payload.getParameter("ALT_VIEW_BARVIEW"), "View data must be removed from normal metadata");
    }

}
