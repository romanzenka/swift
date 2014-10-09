package edu.mayo.mprc.dbcurator.model;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.emory.mathcs.backport.java.util.Arrays;
import edu.mayo.mprc.MprcException;

import java.util.List;

/**
 * @author Raymond Moore
 */
public class StepsMap {
    static final BiMap<String, Class<? extends CurationStep>> typeNames;
    // TODO - dynamically collect all the implementations
    static final List<String> allClasses = Arrays.asList(new Object[]{
            "edu.mayo.mprc.dbcurator.model.curationsteps.DatabaseUploadStep",
            "edu.mayo.mprc.dbcurator.model.curationsteps.HeaderFilterStep",
            "edu.mayo.mprc.dbcurator.model.curationsteps.HeaderTransformStep",
            "edu.mayo.mprc.dbcurator.model.curationsteps.MakeDecoyStep",
            "edu.mayo.mprc.dbcurator.model.curationsteps.ManualInclusionStep",
            "edu.mayo.mprc.dbcurator.model.curationsteps.NewDatabaseInclusion"});

    static {
        typeNames = HashBiMap.create();
        for (String className : allClasses) {
            String typeName = null;
            Class<? extends CurationStep> clazz;
            try {
                clazz = (Class<? extends CurationStep>) Class.forName(className);
                typeName = clazz.newInstance().getStepTypeName();
                typeNames.put(typeName, clazz);
            } catch (Exception e) {
                throw new MprcException("Could not determine type name for class " + className, e);
            }
        }
    }

    public static String getTypeForClass(Class<? extends CurationStep> clazz) {
        String type = typeNames.inverse().get(clazz);
        if (type == null) {
            throw new MprcException("Could not find type for class " + clazz.getName());
        }
        return type;
    }

    public static Class<? extends CurationStep> getClassForType(String type) {
        Class<? extends CurationStep> clazz = typeNames.get(type);
        if (clazz == null) {
            throw new MprcException("Could not find class for type " + type);
        }
        return clazz;
    }
}
