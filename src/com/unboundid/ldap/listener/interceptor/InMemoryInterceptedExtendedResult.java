/*
 * Copyright 2014-2017 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2014-2017 UnboundID Corp.
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
package com.unboundid.ldap.listener.interceptor;



import com.unboundid.ldap.sdk.IntermediateResponse;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ExtendedRequest;
import com.unboundid.ldap.sdk.ExtendedResult;
import com.unboundid.util.NotExtensible;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;



/**
 * This class provides an API that can be used in the course of processing an
 * extended request via the {@link InMemoryOperationInterceptor} API.
 */
@NotExtensible()
@ThreadSafety(level=ThreadSafetyLevel.INTERFACE_NOT_THREADSAFE)
public interface InMemoryInterceptedExtendedResult
       extends InMemoryInterceptedResult
{
  /**
   * Retrieves the extended request that was processed.  If the request was
   * altered between the time it was received from the client and the time it
   * was actually processed by the in-memory directory server, then this will be
   * the most recently altered version.
   *
   * @return  The extended request that was processed.
   */
  ExtendedRequest getRequest();



  /**
   * Retrieves the extended result to be returned to the client.
   *
   * @return  The extended result to be returned to the client.
   */
  ExtendedResult getResult();



  /**
   * Replaces the extended result to be returned to the client.
   *
   * @param  extendedResult  The extended result that should be returned to the
   *                         client instead of the result originally generated
   *                         by the in-memory directory server.  It must not be
   *                         {@code null}.
   */
  void setResult(ExtendedResult extendedResult);



  /**
   * Sends the provided intermediate response message to the client.  It will
   * be processed by the
   * {@link InMemoryOperationInterceptor#processIntermediateResponse} method of
   * all registered operation interceptors.
   *
   * @param  intermediateResponse  The intermediate response to send to the
   *                               client.  It must not be {@code null}.
   *
   * @throws  LDAPException  If a problem is encountered while trying to send
   *                         the intermediate response.
   */
  void sendIntermediateResponse(IntermediateResponse intermediateResponse)
         throws LDAPException;
}
