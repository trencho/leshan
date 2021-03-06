package org.eclipse.leshan.core.attributes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AttributeTest {

    @Test
    public void should_pick_correct_model() {
        Attribute verAttribute = new Attribute(Attribute.OBJECT_VERSION, "1.0");
        assertEquals("ver", verAttribute.getCoRELinkParam());
        assertEquals("1.0", verAttribute.getValue());
        assertTrue(verAttribute.canBeAssignedTo(AssignationLevel.OBJECT));
        assertFalse(verAttribute.isWritable());
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_on_invalid_value_type() {
        new Attribute(Attribute.OBJECT_VERSION, 123);
    }
}
