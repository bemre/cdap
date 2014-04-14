/**
 * Autogenerated by Thrift Compiler (0.8.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package com.continuuity.app.services;

import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;

import org.apache.thrift.scheme.TupleScheme;
import org.apache.thrift.protocol.TTupleProtocol;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Information about resource
 */
public class ArchiveInfo implements org.apache.thrift.TBase<ArchiveInfo, ArchiveInfo._Fields>, java.io.Serializable, Cloneable {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("ArchiveInfo");

  private static final org.apache.thrift.protocol.TField ACCOUNT_ID_FIELD_DESC = new org.apache.thrift.protocol.TField("accountId", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField APPLICATION_ID_FIELD_DESC = new org.apache.thrift.protocol.TField("applicationId", org.apache.thrift.protocol.TType.STRING, (short)2);
  private static final org.apache.thrift.protocol.TField FILENAME_FIELD_DESC = new org.apache.thrift.protocol.TField("filename", org.apache.thrift.protocol.TType.STRING, (short)3);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new ArchiveInfoStandardSchemeFactory());
    schemes.put(TupleScheme.class, new ArchiveInfoTupleSchemeFactory());
  }

  public String accountId; // required
  public String applicationId; // optional
  public String filename; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    ACCOUNT_ID((short)1, "accountId"),
    APPLICATION_ID((short)2, "applicationId"),
    FILENAME((short)3, "filename");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // ACCOUNT_ID
          return ACCOUNT_ID;
        case 2: // APPLICATION_ID
          return APPLICATION_ID;
        case 3: // FILENAME
          return FILENAME;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  private _Fields optionals[] = {_Fields.APPLICATION_ID};
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.ACCOUNT_ID, new org.apache.thrift.meta_data.FieldMetaData("accountId", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.APPLICATION_ID, new org.apache.thrift.meta_data.FieldMetaData("applicationId", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.FILENAME, new org.apache.thrift.meta_data.FieldMetaData("filename", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(ArchiveInfo.class, metaDataMap);
  }

  public ArchiveInfo() {
  }

  public ArchiveInfo(
    String accountId,
    String filename)
  {
    this();
    this.accountId = accountId;
    this.filename = filename;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public ArchiveInfo(ArchiveInfo other) {
    if (other.isSetAccountId()) {
      this.accountId = other.accountId;
    }
    if (other.isSetApplicationId()) {
      this.applicationId = other.applicationId;
    }
    if (other.isSetFilename()) {
      this.filename = other.filename;
    }
  }

  public ArchiveInfo deepCopy() {
    return new ArchiveInfo(this);
  }

  @Override
  public void clear() {
    this.accountId = null;
    this.applicationId = null;
    this.filename = null;
  }

  public String getAccountId() {
    return this.accountId;
  }

  public ArchiveInfo setAccountId(String accountId) {
    this.accountId = accountId;
    return this;
  }

  public void unsetAccountId() {
    this.accountId = null;
  }

  /** Returns true if field accountId is set (has been assigned a value) and false otherwise */
  public boolean isSetAccountId() {
    return this.accountId != null;
  }

  public void setAccountIdIsSet(boolean value) {
    if (!value) {
      this.accountId = null;
    }
  }

  public String getApplicationId() {
    return this.applicationId;
  }

  public ArchiveInfo setApplicationId(String applicationId) {
    this.applicationId = applicationId;
    return this;
  }

  public void unsetApplicationId() {
    this.applicationId = null;
  }

  /** Returns true if field applicationId is set (has been assigned a value) and false otherwise */
  public boolean isSetApplicationId() {
    return this.applicationId != null;
  }

  public void setApplicationIdIsSet(boolean value) {
    if (!value) {
      this.applicationId = null;
    }
  }

  public String getFilename() {
    return this.filename;
  }

  public ArchiveInfo setFilename(String filename) {
    this.filename = filename;
    return this;
  }

  public void unsetFilename() {
    this.filename = null;
  }

  /** Returns true if field filename is set (has been assigned a value) and false otherwise */
  public boolean isSetFilename() {
    return this.filename != null;
  }

  public void setFilenameIsSet(boolean value) {
    if (!value) {
      this.filename = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case ACCOUNT_ID:
      if (value == null) {
        unsetAccountId();
      } else {
        setAccountId((String)value);
      }
      break;

    case APPLICATION_ID:
      if (value == null) {
        unsetApplicationId();
      } else {
        setApplicationId((String)value);
      }
      break;

    case FILENAME:
      if (value == null) {
        unsetFilename();
      } else {
        setFilename((String)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case ACCOUNT_ID:
      return getAccountId();

    case APPLICATION_ID:
      return getApplicationId();

    case FILENAME:
      return getFilename();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case ACCOUNT_ID:
      return isSetAccountId();
    case APPLICATION_ID:
      return isSetApplicationId();
    case FILENAME:
      return isSetFilename();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof ArchiveInfo)
      return this.equals((ArchiveInfo)that);
    return false;
  }

  public boolean equals(ArchiveInfo that) {
    if (that == null)
      return false;

    boolean this_present_accountId = true && this.isSetAccountId();
    boolean that_present_accountId = true && that.isSetAccountId();
    if (this_present_accountId || that_present_accountId) {
      if (!(this_present_accountId && that_present_accountId))
        return false;
      if (!this.accountId.equals(that.accountId))
        return false;
    }

    boolean this_present_applicationId = true && this.isSetApplicationId();
    boolean that_present_applicationId = true && that.isSetApplicationId();
    if (this_present_applicationId || that_present_applicationId) {
      if (!(this_present_applicationId && that_present_applicationId))
        return false;
      if (!this.applicationId.equals(that.applicationId))
        return false;
    }

    boolean this_present_filename = true && this.isSetFilename();
    boolean that_present_filename = true && that.isSetFilename();
    if (this_present_filename || that_present_filename) {
      if (!(this_present_filename && that_present_filename))
        return false;
      if (!this.filename.equals(that.filename))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  public int compareTo(ArchiveInfo other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;
    ArchiveInfo typedOther = (ArchiveInfo)other;

    lastComparison = Boolean.valueOf(isSetAccountId()).compareTo(typedOther.isSetAccountId());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetAccountId()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.accountId, typedOther.accountId);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetApplicationId()).compareTo(typedOther.isSetApplicationId());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetApplicationId()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.applicationId, typedOther.applicationId);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetFilename()).compareTo(typedOther.isSetFilename());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetFilename()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.filename, typedOther.filename);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("ArchiveInfo(");
    boolean first = true;

    sb.append("accountId:");
    if (this.accountId == null) {
      sb.append("null");
    } else {
      sb.append(this.accountId);
    }
    first = false;
    if (isSetApplicationId()) {
      if (!first) sb.append(", ");
      sb.append("applicationId:");
      if (this.applicationId == null) {
        sb.append("null");
      } else {
        sb.append(this.applicationId);
      }
      first = false;
    }
    if (!first) sb.append(", ");
    sb.append("filename:");
    if (this.filename == null) {
      sb.append("null");
    } else {
      sb.append(this.filename);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    if (accountId == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'accountId' was not present! Struct: " + toString());
    }
    if (filename == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'filename' was not present! Struct: " + toString());
    }
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class ArchiveInfoStandardSchemeFactory implements SchemeFactory {
    public ArchiveInfoStandardScheme getScheme() {
      return new ArchiveInfoStandardScheme();
    }
  }

  private static class ArchiveInfoStandardScheme extends StandardScheme<ArchiveInfo> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, ArchiveInfo struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // ACCOUNT_ID
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.accountId = iprot.readString();
              struct.setAccountIdIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // APPLICATION_ID
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.applicationId = iprot.readString();
              struct.setApplicationIdIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // FILENAME
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.filename = iprot.readString();
              struct.setFilenameIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, ArchiveInfo struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.accountId != null) {
        oprot.writeFieldBegin(ACCOUNT_ID_FIELD_DESC);
        oprot.writeString(struct.accountId);
        oprot.writeFieldEnd();
      }
      if (struct.applicationId != null) {
        if (struct.isSetApplicationId()) {
          oprot.writeFieldBegin(APPLICATION_ID_FIELD_DESC);
          oprot.writeString(struct.applicationId);
          oprot.writeFieldEnd();
        }
      }
      if (struct.filename != null) {
        oprot.writeFieldBegin(FILENAME_FIELD_DESC);
        oprot.writeString(struct.filename);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class ArchiveInfoTupleSchemeFactory implements SchemeFactory {
    public ArchiveInfoTupleScheme getScheme() {
      return new ArchiveInfoTupleScheme();
    }
  }

  private static class ArchiveInfoTupleScheme extends TupleScheme<ArchiveInfo> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, ArchiveInfo struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      oprot.writeString(struct.accountId);
      oprot.writeString(struct.filename);
      BitSet optionals = new BitSet();
      if (struct.isSetApplicationId()) {
        optionals.set(0);
      }
      oprot.writeBitSet(optionals, 1);
      if (struct.isSetApplicationId()) {
        oprot.writeString(struct.applicationId);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, ArchiveInfo struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      struct.accountId = iprot.readString();
      struct.setAccountIdIsSet(true);
      struct.filename = iprot.readString();
      struct.setFilenameIsSet(true);
      BitSet incoming = iprot.readBitSet(1);
      if (incoming.get(0)) {
        struct.applicationId = iprot.readString();
        struct.setApplicationIdIsSet(true);
      }
    }
  }

}

