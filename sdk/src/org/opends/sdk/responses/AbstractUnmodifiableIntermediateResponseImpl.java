/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE
 * or https://OpenDS.dev.java.net/OpenDS.LICENSE.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2010 Sun Microsystems, Inc.
 */

package org.opends.sdk.responses;

import org.opends.sdk.ByteString;

/**
 * An abstract unmodifiable Intermediate response which can be used as the basis
 * for implementing new unmodifiable Intermediate responses.
 *
 * @param <S>
 *          The type of Intermediate response.
 */
abstract class AbstractUnmodifiableIntermediateResponseImpl
    <S extends IntermediateResponse> extends AbstractUnmodifiableResponseImpl<S>
    implements IntermediateResponse
{
  protected AbstractUnmodifiableIntermediateResponseImpl(S impl) {
    super(impl);
  }

  @Override
  public String getOID() {
    return impl.getOID();
  }

  @Override
  public ByteString getValue() {
    return impl.getValue();
  }

  @Override
  public boolean hasValue() {
    return impl.hasValue();
  }
}
