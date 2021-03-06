/*******************************************************************************
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Boya Zhang - initial API and implementation
 *******************************************************************************/

package org.eclipse.leshan.senml.json.minimaljson;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.util.Base64;
import org.eclipse.leshan.senml.SenMLPack;
import org.eclipse.leshan.senml.SenMLRecord;

public class SenMLJsonPackSerDes {

    public byte[] serializeToJson(SenMLPack pack) {
        JsonArray jsonArray = new JsonArray();

        for (SenMLRecord record : pack.getRecords()) {
            JsonObject jsonObj = new JsonObject();

            if (record.getBaseName() != null && record.getBaseName().length() > 0) {
                jsonObj.add("bn", record.getBaseName());
            }

            if (record.getBaseTime() != null) {
                jsonObj.add("bt", record.getBaseTime());
            }

            if (record.getName() != null && record.getName().length() > 0) {
                jsonObj.add("n", record.getName());
            }

            if (record.getTime() != null) {
                jsonObj.add("t", record.getTime());
            }

            Type type = record.getType();
            if (type != null) {
                switch (record.getType()) {
                case FLOAT:
                    jsonObj.add("v", record.getFloatValue().doubleValue());
                    break;
                case BOOLEAN:
                    jsonObj.add("vb", record.getBooleanValue());
                    break;
                case OBJLNK:
                    jsonObj.add("vlo", record.getObjectLinkValue());
                    break;
                case OPAQUE:
                    jsonObj.add("vd", Base64.encodeBase64String(record.getOpaqueValue()));
                case STRING:
                    jsonObj.add("vs", record.getStringValue());
                    break;
                default:
                    break;
                }
            }

            jsonArray.add(jsonObj);
        }

        return jsonArray.toString().getBytes();
    }

    public SenMLPack deserializeFromJson(JsonArray array) {

        if (array == null)
            return null;

        SenMLPack pack = new SenMLPack();

        for (JsonValue value : array.values()) {
            JsonObject o = value.asObject();

            SenMLRecord record = new SenMLRecord();

            JsonValue bn = o.get("bn");
            if (bn != null && bn.isString())
                record.setBaseName(bn.asString());

            JsonValue bt = o.get("bt");
            if (bt != null && bt.isNumber())
                record.setBaseTime(bt.asLong());

            JsonValue n = o.get("n");
            if (n != null && n.isString())
                record.setName(n.asString());

            JsonValue t = o.get("t");
            if (t != null && t.isNumber())
                record.setTime(t.asLong());

            JsonValue v = o.get("v");
            if (v != null && v.isNumber())
                record.setFloatValue(v.asDouble());

            JsonValue vb = o.get("vb");
            if (vb != null && vb.isBoolean())
                record.setBooleanValue(vb.asBoolean());

            JsonValue vs = o.get("vs");
            if (vs != null && vs.isString())
                record.setStringValue(vs.asString());

            JsonValue vlo = o.get("vlo");
            if (vlo != null && vlo.isString())
                record.setObjectLinkValue(vlo.asString());

            JsonValue vd = o.get("vd");
            if (vd != null && vd.isString())
                record.setOpaqueValue(Base64.decodeBase64(vd.asString()));

            pack.addRecord(record);
        }

        return pack;
    }
}
