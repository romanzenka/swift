package edu.mayo.mprc.dbcurator.model.curationsteps;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.dbcurator.model.StepsMap;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Raymond Moore
 */
public class StepsMapTest {
    @Test
    public void testGetTypeForClass() throws Exception {
        Assert.assertEquals(StepsMap.getTypeForClass(NewDatabaseInclusion.class), "new_db");
    }

    @Test
    public void testGetClassForType() throws Exception {
        Assert.assertEquals(StepsMap.getClassForType("make_decoy"), MakeDecoyStep.class);
    }

    @Test(expectedExceptions = MprcException.class)
    public void shouldFailForMissingType() {
        StepsMap.getClassForType("missing_type");
    }
}
