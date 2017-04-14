/*
 * Copyright 2009-2017 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2015-2017 UnboundID Corp.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package com.unboundid.ldap.sdk.unboundidds.extensions;



import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.unboundid.asn1.ASN1Element;
import com.unboundid.asn1.ASN1Enumerated;
import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.asn1.ASN1Sequence;
import com.unboundid.asn1.ASN1Set;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.IntermediateResponse;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.NotMutable;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;

import static com.unboundid.ldap.sdk.unboundidds.extensions.ExtOpMessages.*;
import static com.unboundid.util.Debug.*;
import static com.unboundid.util.StaticUtils.*;



/**
 * This class provides an implementation of the stream proxy values intermediate
 * response, which may be used to provide a partial or complete list of the
 * values for a specified attribute, or DNs of entries contained in a specified
 * portion of the server DIT.
 * <BR>
 * <BLOCKQUOTE>
 *   <B>NOTE:</B>  This class, and other classes within the
 *   {@code com.unboundid.ldap.sdk.unboundidds} package structure, are only
 *   supported for use against Ping Identity, UnboundID, and Alcatel-Lucent 8661
 *   server products.  These classes provide support for proprietary
 *   functionality or for external specifications that are not considered stable
 *   or mature enough to be guaranteed to work in an interoperable way with
 *   other types of LDAP servers.
 * </BLOCKQUOTE>
 * <BR>
 * This intermediate response has an OID of "1.3.6.1.4.1.30221.2.6.9" and the
 * value is encoded as follows:
 * <PRE>
 *   StreamProxyValuesIntermediateResponse ::= SEQUENCE {
 *        attributeName         [0] LDAPString OPTIONAL,
 *        result                [1] ENUMERATED {
 *             allValuesReturned       (0),
 *             moreValuesToReturn      (1),
 *             attributeNotIndexed     (2),
 *             processingError         (3),
 *             ... },
 *        diagnosticMessage     [2] OCTET STRING OPTIONAL,
 *        values                [4] SET OF BackendSetValue OPTIONAL,
 *        ... }
 *
 *   BackendSetValue ::= SEQUENCE {
 *        backendSetID     OCTET STRING,
 *        value            OCTET STRING }
 * </PRE>
 */
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public final class StreamProxyValuesIntermediateResponse
       extends IntermediateResponse
{
  /**
   * The OID (1.3.6.1.4.1.30221.2.6.9) for the get stream proxy values
   * intermediate response.
   */
  public static final String STREAM_PROXY_VALUES_INTERMEDIATE_RESPONSE_OID =
       "1.3.6.1.4.1.30221.2.6.9";



  /**
   * The integer value for the "all values returned" result.
   */
  public static final int RESULT_ALL_VALUES_RETURNED = 0;



  /**
   * The integer value for the "more values to return" result.
   */
  public static final int RESULT_MORE_VALUES_TO_RETURN = 1;



  /**
   * The integer value for the "attribute not indexed" result.
   */
  public static final int RESULT_ATTRIBUTE_NOT_INDEXED = 2;



  /**
   * The integer value for the "processing error" result.
   */
  public static final int RESULT_PROCESSING_ERROR = 3;



  /**
   * The BER type for the attribute name element.
   */
  private static final byte TYPE_ATTRIBUTE_NAME = (byte) 0x80;



  /**
   * The BER type for the result element.
   */
  private static final byte TYPE_RESULT = (byte) 0x81;



  /**
   * The BER type for the diagnostic message element.
   */
  private static final byte TYPE_DIAGNOSTIC_MESSAGE = (byte) 0x82;



  /**
   * The BER type for the values element.
   */
  private static final byte TYPE_VALUES = (byte) 0xA4;



  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 6861844092877880224L;



  // The result code for this stream proxy values intermediate response.
  private final int result;

  // The list of values for this stream proxy values intermediate response.
  private final List<StreamProxyValuesBackendSetValue> values;

  // The attribute name for this stream proxy values intermediate response, if
  // any.
  private final String attributeName;

  // The diagnostic message for this stream proxy values intermediate response,
  // if any.
  private final String diagnosticMessage;



  /**
   * Creates a new stream proxy values intermediate response with the
   * provided information.
   *
   * @param  attributeName      The name of the attribute with which the
   *                            included values are associated.  This may be
   *                            {@code null} if the provided values are DNs.
   * @param  result             The integer value that provides information
   *                            about the state of the stream proxy values
   *                            response.
   * @param  diagnosticMessage  The diagnostic message that provides more
   *                            information about the result, or {@code null} if
   *                            none is required.
   * @param  values             The set of values included in this stream proxy
   *                            values intermediate response.  It may be
   *                            {@code null} or empty if this is an error
   *                            result, or there are no values of the specified
   *                            type in the server.
   * @param  controls           The set of controls to include in this
   *                            intermediate response.  It may be {@code null}
   *                            or empty if there should not be any controls.
   */
  public StreamProxyValuesIntermediateResponse(final String attributeName,
              final int result, final String diagnosticMessage,
              final Collection<StreamProxyValuesBackendSetValue> values,
              final Control... controls)
  {
    super(STREAM_PROXY_VALUES_INTERMEDIATE_RESPONSE_OID,
          encodeValue(attributeName, result, diagnosticMessage, values),
          controls);

    this.attributeName     = attributeName;
    this.result            = result;
    this.diagnosticMessage = diagnosticMessage;

    if ((values == null) || values.isEmpty())
    {
      this.values = Collections.emptyList();
    }
    else
    {
      this.values = Collections.unmodifiableList(
           new ArrayList<StreamProxyValuesBackendSetValue>(values));
    }
  }



  /**
   * Creates a new stream proxy values intermediate response with
   * information from the provided generic intermediate response.
   *
   * @param  intermediateResponse  The generic intermediate response that should
   *                               be used to create this new intermediate
   *                               response.
   *
   * @throws  LDAPException  If the provided intermediate response cannot be
   *                         parsed as a stream proxy values intermediate
   *                         response.
   */
  public StreamProxyValuesIntermediateResponse(
                 final IntermediateResponse intermediateResponse)
         throws LDAPException
  {
    super(intermediateResponse);

    final ASN1OctetString value = intermediateResponse.getValue();
    if (value == null)
    {
      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_STREAM_PROXY_VALUES_RESPONSE_NO_VALUE.get());
    }

    int    tmpResult  = -1;
    String tmpAttr    = null;
    String tmpMessage = null;
    final ArrayList<StreamProxyValuesBackendSetValue> tmpValues =
         new ArrayList<StreamProxyValuesBackendSetValue>();

    try
    {
      final ASN1Element[] elements =
           ASN1Element.decode(value.getValue()).decodeAsSequence().elements();
      for (final ASN1Element e : elements)
      {
        switch (e.getType())
        {
          case TYPE_ATTRIBUTE_NAME:
            tmpAttr = e.decodeAsOctetString().stringValue();
            break;
          case TYPE_RESULT:
            tmpResult = e.decodeAsEnumerated().intValue();
            if (tmpResult < 0)
            {
              throw new LDAPException(ResultCode.DECODING_ERROR,
                   ERR_STREAM_PROXY_VALUES_RESPONSE_INVALID_RESULT.get(
                        tmpResult));
            }
            break;
          case TYPE_DIAGNOSTIC_MESSAGE:
            tmpMessage = e.decodeAsOctetString().stringValue();
            break;
          case TYPE_VALUES:
            final ASN1Element[] valueElements = e.decodeAsSet().elements();
            for (final ASN1Element ve : valueElements)
            {
              tmpValues.add(StreamProxyValuesBackendSetValue.decode(ve));
            }
            break;
          default:
            throw new LDAPException(ResultCode.DECODING_ERROR,
                 ERR_STREAM_PROXY_VALUES_RESPONSE_INVALID_SEQUENCE_TYPE.get(
                      toHex(e.getType())));
        }
      }
    }
    catch (final LDAPException le)
    {
      throw le;
    }
    catch (final Exception e)
    {
      debugException(e);
      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_STREAM_PROXY_VALUES_RESPONSE_CANNOT_DECODE.get(
                getExceptionMessage(e)), e);
    }

    if (tmpResult < 0)
    {
      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_STREAM_PROXY_VALUES_RESPONSE_NO_RESULT.get());
    }

    attributeName     = tmpAttr;
    result            = tmpResult;
    diagnosticMessage = tmpMessage;
    values            = Collections.unmodifiableList(tmpValues);
  }



  /**
   * Encodes the provided information in a form suitable for use as the value of
   * this intermediate response.
   *
   * @param  attributeName      The name of the attribute with which the
   *                            included values are associated.  This may be
   *                            {@code null} if the provided values are DNs.
   * @param  result             The integer value that provides information
   *                            about the state of the stream proxy values
   *                            response.
   * @param  diagnosticMessage  The diagnostic message that provides more
   *                            information about the result, or {@code null} if
   *                            none is required.
   * @param  values             The set of values included in this stream
   *                            proxy values intermediate response.  It may
   *                            be {@code null} or empty if this is an error
   *                            result, or there are no values of the specified
   *                            type in the server.
   *
   * @return  An ASN.1 octet string containing the encoded value to use for this
   *          intermediate response.
   */
  private static ASN1OctetString encodeValue(final String attributeName,
               final int result, final String diagnosticMessage,
               final Collection<StreamProxyValuesBackendSetValue> values)
  {
    final ArrayList<ASN1Element> elements = new ArrayList<ASN1Element>(4);

    if (attributeName != null)
    {
      elements.add(new ASN1OctetString(TYPE_ATTRIBUTE_NAME, attributeName));
    }

    elements.add(new ASN1Enumerated(TYPE_RESULT, result));

    if (diagnosticMessage != null)
    {
      elements.add(new ASN1OctetString(TYPE_DIAGNOSTIC_MESSAGE,
                                       diagnosticMessage));
    }

    if ((values != null) && (! values.isEmpty()))
    {
      final ArrayList<ASN1Element> valueElements =
           new ArrayList<ASN1Element>(values.size());
      for (final StreamProxyValuesBackendSetValue v : values)
      {
        valueElements.add(v.encode());
      }

      elements.add(new ASN1Set(TYPE_VALUES, valueElements));
    }

    return new ASN1OctetString(new ASN1Sequence(elements).encode());
  }



  /**
   * Retrieves the name of the attribute with which this stream proxy values
   * intermediate response is associated.
   *
   * @return  The name of the attribute with which this stream proxy values
   *          intermediate response is associated, or {@code null} if the values
   *          are entry DNs rather than attribute values.
   */
  public String getAttributeName()
  {
    return attributeName;
  }



  /**
   * Retrieves the integer value of the result for this stream proxy values
   * intermediate response.
   *
   * @return  The integer value of the result for this stream proxy values
   *          intermediate response.
   */
  public int getResult()
  {
    return result;
  }



  /**
   * Retrieves the diagnostic message for this stream proxy values intermediate
   * response.
   *
   * @return  The diagnostic message for this stream proxy values intermediate
   *          response, or {@code null} if there is none.
   */
  public String getDiagnosticMessage()
  {
    return diagnosticMessage;
  }



  /**
   * Retrieves the list of values for this stream proxy values intermediate
   * response.
   *
   * @return  The list of values for this stream proxy values intermediate
   *          response, or an empty list if there are no values.
   */
  public List<StreamProxyValuesBackendSetValue> getValues()
  {
    return values;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getIntermediateResponseName()
  {
    return INFO_INTERMEDIATE_RESPONSE_NAME_STREAM_PROXY_VALUES.get();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String valueToString()
  {
    final StringBuilder buffer = new StringBuilder();

    if (attributeName != null)
    {
      buffer.append("attributeName='");
      buffer.append(attributeName);
      buffer.append("' ");
    }

    buffer.append("result='");
    switch (result)
    {
      case RESULT_ALL_VALUES_RETURNED:
        buffer.append("all values returned");
        break;
      case RESULT_ATTRIBUTE_NOT_INDEXED:
        buffer.append("attribute not indexed");
        break;
      case RESULT_MORE_VALUES_TO_RETURN:
        buffer.append("more values to return");
        break;
      case RESULT_PROCESSING_ERROR:
        buffer.append("processing error");
        break;
      default:
        buffer.append(result);
        break;
    }
    buffer.append('\'');

    if (diagnosticMessage != null)
    {
      buffer.append(" diagnosticMessage='");
      buffer.append(diagnosticMessage);
      buffer.append('\'');
    }

    buffer.append(" valueCount='");
    buffer.append(values.size());
    buffer.append('\'');

    return buffer.toString();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void toString(final StringBuilder buffer)
  {
    buffer.append("StreamProxyValuesIntermediateResponse(");

    final int messageID = getMessageID();
    if (messageID >= 0)
    {
      buffer.append("messageID=");
      buffer.append(messageID);
      buffer.append(", ");
    }

    if (attributeName != null)
    {
      buffer.append("attributeName='");
      buffer.append(attributeName);
      buffer.append("', ");
    }

    buffer.append("result=");
    buffer.append(result);

    if (diagnosticMessage != null)
    {
      buffer.append(", diagnosticMessage='");
      buffer.append(diagnosticMessage);
      buffer.append('\'');
    }

    buffer.append(", values={");

    final Iterator<StreamProxyValuesBackendSetValue> iterator =
         values.iterator();
    while (iterator.hasNext())
    {
      iterator.next().toString(buffer);
      if (iterator.hasNext())
      {
        buffer.append(", ");
      }
    }

    final Control[] controls = getControls();
    if (controls.length > 0)
    {
      buffer.append(", controls={");
      for (int i=0; i < controls.length; i++)
      {
        if (i > 0)
        {
          buffer.append(", ");
        }

        buffer.append(controls[i]);
      }
      buffer.append('}');
    }

    buffer.append("})");
  }
}
