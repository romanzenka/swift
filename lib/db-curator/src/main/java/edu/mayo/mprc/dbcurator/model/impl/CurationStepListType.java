package edu.mayo.mprc.dbcurator.model.impl;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.DummyFileTokenTranslator;
import edu.mayo.mprc.database.FileTokenToDatabaseTranslator;
import edu.mayo.mprc.dbcurator.model.CurationStep;
import edu.mayo.mprc.dbcurator.model.StepsMap;
import org.hibernate.HibernateException;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.usertype.UserType;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Raymond Moore
 */
public class CurationStepListType implements UserType {
    private FileTokenToDatabaseTranslator translator;
    Gson gson ;

    private int stepVersion = 1;

    public CurationStepListType(){}
    public CurationStepListType(FileTokenToDatabaseTranslator translator) {
        this.translator = translator;
        //gson = new GsonBuilder().registerTypeAdapter(File.class, ).create();

    }

    @Override
    public int[] sqlTypes() {
        return new int[]{StandardBasicTypes.STRING.sqlType()};
    }

    @Override
    public Class returnedClass() {
        return List.class;
    }

    @Override
    public boolean equals(Object o, Object o2) throws HibernateException {
        if (o == o2) {
            return true;
        }
        if (o == null || o2 == null) {
            return false;
        }
        return o.equals(o2);
    }

    @Override
    public int hashCode(Object o) throws HibernateException {
        return o.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet resultSet, String[] strings, Object o) throws HibernateException, SQLException {
        final String uriString = resultSet.getString(strings[0]);

        if (resultSet.wasNull()) {
            return null;
        }
        if (uriString == null) {
            return null;
        }

        try {
            return assemble(uriString, null);
        } catch (Exception t) {
            throw new HibernateException(t);
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index) throws HibernateException, SQLException {
        if (null == value) {
            preparedStatement.setNull(index, Types.VARCHAR);
        } else {
            checkTranslatorNotNull();
            preparedStatement.setString(index, translator.fileToDatabaseToken((File) value));
        }    }
    private void checkTranslatorNotNull() {
        if (translator == null) {
            throw new MprcException(getClass().getName() + " was not initialized with a translator for file tokens.\nUse for instance " + DummyFileTokenTranslator.class.getName() + " before you start storing file paths to database.");
        }
    }

    @Override
    public Object deepCopy(Object o) throws HibernateException {
        if (o == null) {
            return null;
        }
        return new File(((File) o).getAbsoluteFile().toURI());
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(Object o) throws HibernateException {
        List<CurationStep> curationSteps = (List<CurationStep>) o;
        JsonObject reqObj = new JsonObject();
        reqObj.addProperty( "version", stepVersion );
        JsonArray typeAppendedSteps = new JsonArray();
        for(CurationStep g : curationSteps){
            JsonObject jStep = gson.toJsonTree(g).getAsJsonObject();
            jStep.addProperty("step_type", g.getStepTypeName());
            typeAppendedSteps.add(jStep);
        }
        reqObj.add( "steps", typeAppendedSteps );
        return gson.toJson(reqObj);
    }

    @Override
    public Object assemble(Serializable serializable, Object o) throws HibernateException {

        Type listType = new TypeToken<List<CurationStep>>(){}.getType();
        JsonObject jobj = new Gson().fromJson(getCurationStepsJson(), JsonObject.class);
        List<CurationStep> myModelList = new ArrayList<CurationStep>();
        if( jobj == null ){
            //      stepVersion = 0;
        }
        else{
            //    stepVersion = jobj.get("version").getAsInt();
            JsonArray stepArray = jobj.get("steps").getAsJsonArray();
            for(JsonElement j : stepArray){
                Class mySource = StepsMap.getClassForType(j.getAsJsonObject().get("step_type").getAsString());
                myModelList.add( (CurationStep) gson.fromJson(j, mySource)  );
            }
        }
        return myModelList;

    }

    @Override
    public Object replace(Object o, Object o2, Object o3) throws HibernateException {
        return o;
    }

}
