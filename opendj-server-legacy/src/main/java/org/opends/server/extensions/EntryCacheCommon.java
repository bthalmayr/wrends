/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2006-2008 Sun Microsystems, Inc.
 * Portions Copyright 2014-2016 ForgeRock AS.
 */
package org.opends.server.extensions;

import static org.opends.server.util.StaticUtils.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.i18n.LocalizableMessageDescriptor;
import org.forgerock.opendj.ldap.DN;
import org.opends.server.types.DirectoryException;
import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.forgerock.opendj.ldap.ResultCode;
import org.opends.server.api.MonitorData;
import org.opends.server.types.SearchFilter;

/** This class provides some common tools to all entry cache implementations. */
public class EntryCacheCommon
{

  private static final LocalizedLogger logger = LocalizedLogger.getLoggerForThisClass();

  /**
   * Configuration phases. Each value identifies a configuration step:
   * - PHASE_INIT       when invoking method initializeEntryCache()
   * - PHASE_ACCEPTABLE when invoking method isConfigurationChangeAcceptable()
   * - PHASE_APPLY      when invoking method applyConfigurationChange()
   */
  public static enum ConfigPhase
  {
    /** Indicates that entry cache is in initialization check phase. */
    PHASE_INIT,
    /** Indicates that entry cache is in configuration check phase. */
    PHASE_ACCEPTABLE,
    /** Indicates that entry cache is applying its configuration. */
    PHASE_APPLY
  }

  /**
   * Error handler used by local methods to report configuration error.
   * The error handler simplifies the code of initializeEntryCache(),
   * isConfigurationChangeAcceptable() and applyConfigurationChanges() methods.
   */
  public class ConfigErrorHandler
  {
    /** Configuration phase. */
    private EntryCacheCommon.ConfigPhase _configPhase;

    /** Unacceptable reasons. Used when _configPhase is PHASE_ACCEPTABLE. */
    private List<LocalizableMessage> _unacceptableReasons;

    /** Error messages. Used when _configPhase is PHASE_APPLY. */
    private List<LocalizableMessage> _errorMessages;

    /** Result code. Used when _configPhase is PHASE_APPLY. */
    private ResultCode _resultCode;

    /**
     * Acceptable Configuration ? Used when _configPhase is PHASE_ACCEPTABLE
     * or PHASE_APPLY.
     */
    private boolean _isAcceptable;

    /**
     * Indicates whether administrative action is required or not. Used when
     * _configPhase is PHASE_APPLY.
     */
    private boolean _isAdminActionRequired;

    /**
     * Create an error handler.
     *
     * @param configPhase          the configuration phase for which the
     *                             error handler is used
     * @param unacceptableReasons  the reasons why the configuration cannot
     *                             be applied (during PHASE_ACCEPTABLE phase)
     * @param errorMessages        the errors found when applying a new
     *                             configuration (during PHASE_APPLY phase)
     */
    public ConfigErrorHandler (
        EntryCacheCommon.ConfigPhase configPhase,
        List<LocalizableMessage> unacceptableReasons,
        List<LocalizableMessage> errorMessages
        )
    {
      _configPhase           = configPhase;
      _unacceptableReasons   = unacceptableReasons;
      _errorMessages         = errorMessages;
      _resultCode            = ResultCode.SUCCESS;
      _isAcceptable          = true;
      _isAdminActionRequired = false;
    }

    /**
     * Report an error.
     *
     * @param error        the error to report
     * @param isAcceptable <code>true</code> if the configuration is acceptable
     * @param resultCode   the change result for the current configuration
     */
    public void reportError(
            LocalizableMessage error,
            boolean isAcceptable,
            ResultCode resultCode
    )
    {
      switch (_configPhase)
      {
      case PHASE_INIT:
        {
        _errorMessages.add (error);
        _isAcceptable = isAcceptable;
        break;
        }
      case PHASE_ACCEPTABLE:
        {
        _unacceptableReasons.add (error);
        _isAcceptable = isAcceptable;
        break;
        }
      case PHASE_APPLY:
        {
        _errorMessages.add (error);
        _isAcceptable = isAcceptable;
        if (_resultCode == ResultCode.SUCCESS)
        {
          _resultCode = resultCode;
        }
        break;
        }
      }
    }

    /**
     * Report an error.
     *
     * @param error        the error to report
     * @param isAcceptable <code>true</code> if the configuration is acceptable
     * @param resultCode   the change result for the current configuration
     * @param isAdminActionRequired <code>true</code> if administrative action
     *                              is required or <code>false</code> otherwise
     */
    public void reportError(
            LocalizableMessage error,
            boolean isAcceptable,
            ResultCode resultCode,
            boolean isAdminActionRequired
    )
    {
      switch (_configPhase)
      {
      case PHASE_INIT:
        {
        logger.error(error);
        break;
        }
      case PHASE_ACCEPTABLE:
        {
        _unacceptableReasons.add (error);
        _isAcceptable = isAcceptable;
        break;
        }
      case PHASE_APPLY:
        {
        _errorMessages.add (error);
        _isAcceptable = isAcceptable;
        if (_resultCode == ResultCode.SUCCESS)
        {
          _resultCode = resultCode;
        }
        _isAdminActionRequired = isAdminActionRequired;
        break;
        }
      }
    }

    /**
     * Get the current result code that was elaborated right after a
     * configuration has been applied.
     *
     * @return the current result code
     */
    public ResultCode getResultCode()
    {
      return _resultCode;
    }

    /**
     * Get the current isAcceptable flag. The isAcceptable flag is elaborated
     * right after the configuration was checked.
     *
     * @return the isAcceptable flag
     */
    public boolean getIsAcceptable()
    {
      return _isAcceptable;
    }

    /**
     * Get the current unacceptable reasons. The unacceptable reasons are
     * elaborated when the configuration is checked.
     *
     * @return the list of unacceptable reasons
     */
    public List<LocalizableMessage> getUnacceptableReasons()
    {
      return _unacceptableReasons;
    }

    /**
     * Get the current error messages. The error messages are elaborated
     * when the configuration is applied.
     *
     * @return the list of error messages
     */
    public List<LocalizableMessage> getErrorMessages()
    {
      return _errorMessages;
    }

    /**
     * Get the current configuration phase. The configuration phase indicates
     * whether the entry cache is in initialization step, or in configuration
     * checking step or in configuration being applied step.
     *
     * @return the current configuration phase.
     */
    public ConfigPhase getConfigPhase()
    {
      return _configPhase;
    }

    /**
     * Get the current isAdminActionRequired flag as determined after apply
     * action has been taken on a given configuration.
     *
     * @return the isAdminActionRequired flag
     */
    public boolean getIsAdminActionRequired()
    {
      return _isAdminActionRequired;
    }
  } // ConfigErrorHandler


  /**
   * Reads a list of string filters and convert it to a list of search
   * filters.
   *
   * @param filters  the list of string filter to convert to search filters
   * @param decodeErrorMsg  the error message ID to use in case of error
   * @param errorHandler  error handler to report filter decoding errors on
   * @param configEntryDN  the entry cache configuration DN
   *
   * @return the set of search filters
   */
  public static Set<SearchFilter> getFilters(SortedSet<String> filters,
      LocalizableMessageDescriptor.Arg3<Object, Object, Object> decodeErrorMsg,
      ConfigErrorHandler errorHandler, DN configEntryDN)
  {
    // Returned value
    Set<SearchFilter> searchFilters = new HashSet<>();

    // Convert the string filters to search filters.
    if (filters != null && ! filters.isEmpty())
    {
      for (String curFilter: filters)
      {
        try
        {
          searchFilters.add (SearchFilter.createFilterFromString (curFilter));
        }
        catch (DirectoryException de)
        {
          // We couldn't decode this filter. Report an error and continue.
          LocalizableMessage message = decodeErrorMsg.get(String.valueOf(configEntryDN),
            curFilter, de.getMessage() != null ? de.getMessage() :
              stackTraceToSingleLineString(de));
          errorHandler.reportError(message, false,
            ResultCode.INVALID_ATTRIBUTE_SYNTAX);
        }
      }
    }

    // done
    return searchFilters;
  }


  /**
   * Create a new error handler.
   *
   * @param configPhase          the configuration phase for which the
   *                             error handler is used
   * @param unacceptableReasons  the reasons why the configuration cannot
   *                             be applied (during PHASE_ACCEPTABLE phase)
   * @param errorMessages        the errors found when applying a new
   *                             configuration (during PHASE_APPLY phase)
   *
   * @return a new configuration error handler
   */
  public static ConfigErrorHandler getConfigErrorHandler (
      EntryCacheCommon.ConfigPhase  configPhase,
      List<LocalizableMessage> unacceptableReasons,
      List<LocalizableMessage> errorMessages
      )
  {
    EntryCacheCommon ec = new EntryCacheCommon();
    return ec.new ConfigErrorHandler(
        configPhase, unacceptableReasons, errorMessages);
  }


  /**
   * Constructs a set of generic attributes containing entry cache
   * monitor data. Note that <code>null</code> can be passed in
   * place of any argument to denote the argument is omitted, such
   * is when no state data of a given kind is available or can be
   * provided.
   *
   * @param cacheHits      number of cache hits.
   * @param cacheMisses    number of cache misses.
   * @param cacheSize      size of the current cache, in bytes.
   * @param maxCacheSize   maximum allowed cache size, in bytes.
   * @param cacheCount     number of entries stored in the cache.
   * @param maxCacheCount  maximum number of cache entries allowed.
   *
   * @return  A set of generic attributes containing monitor data.
   */
  public static MonitorData getGenericMonitorData(
    Long cacheHits,
    Long cacheMisses,
    Long cacheSize,
    Long maxCacheSize,
    Long cacheCount,
    Long maxCacheCount)
  {
    MonitorData attrs = new MonitorData();

    if (cacheHits != null)
    {
      attrs.add("entryCacheHits", cacheHits);

      // Cache misses is required to get cache tries and hit ratio.
      if (cacheMisses != null)
      {
        Long cacheTries = cacheHits + cacheMisses;
        attrs.add("entryCacheTries", cacheTries);

        Double hitRatioRaw = cacheTries > 0 ? cacheHits.doubleValue()
            / cacheTries.doubleValue() : cacheHits.doubleValue() / 1;
        Double hitRatio = hitRatioRaw * 100D;
        attrs.add("entryCacheHitRatio", hitRatio);
      }
    }

    attrs.addIfNotNull("currentEntryCacheSize", cacheSize);
    attrs.addIfNotNull("maxEntryCacheSize", maxCacheSize);
    attrs.addIfNotNull("currentEntryCacheCount", cacheCount);
    attrs.addIfNotNull("maxEntryCacheCount", maxCacheCount);

    return attrs;
  }
}
