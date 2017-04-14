/*
 * Copyright 2016-2017 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2016-2017 UnboundID Corp.
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
package com.unboundid.ldap.sdk.unboundidds.tools;



import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.AddRequest;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.DeleteRequest;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.ExtendedResult;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPRequest;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.ldap.sdk.ModifyDNRequest;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.UnsolicitedNotificationHandler;
import com.unboundid.ldap.sdk.Version;
import com.unboundid.ldap.sdk.controls.AssertionRequestControl;
import com.unboundid.ldap.sdk.controls.AuthorizationIdentityRequestControl;
import com.unboundid.ldap.sdk.controls.ManageDsaITRequestControl;
import com.unboundid.ldap.sdk.controls.PermissiveModifyRequestControl;
import com.unboundid.ldap.sdk.controls.PostReadRequestControl;
import com.unboundid.ldap.sdk.controls.PreReadRequestControl;
import com.unboundid.ldap.sdk.controls.ProxiedAuthorizationV1RequestControl;
import com.unboundid.ldap.sdk.controls.ProxiedAuthorizationV2RequestControl;
import com.unboundid.ldap.sdk.controls.SimplePagedResultsControl;
import com.unboundid.ldap.sdk.controls.SubtreeDeleteRequestControl;
import com.unboundid.ldap.sdk.controls.TransactionSpecificationRequestControl;
import com.unboundid.ldap.sdk.extensions.StartTransactionExtendedRequest;
import com.unboundid.ldap.sdk.extensions.StartTransactionExtendedResult;
import com.unboundid.ldap.sdk.extensions.EndTransactionExtendedRequest;
import com.unboundid.ldap.sdk.unboundidds.controls.AssuredReplicationLocalLevel;
import com.unboundid.ldap.sdk.unboundidds.controls.
            AssuredReplicationRequestControl;
import com.unboundid.ldap.sdk.unboundidds.controls.
            AssuredReplicationRemoteLevel;
import com.unboundid.ldap.sdk.unboundidds.controls.
            GetAuthorizationEntryRequestControl;
import com.unboundid.ldap.sdk.unboundidds.controls.
            GetUserResourceLimitsRequestControl;
import com.unboundid.ldap.sdk.unboundidds.controls.HardDeleteRequestControl;
import com.unboundid.ldap.sdk.unboundidds.controls.
            IgnoreNoUserModificationRequestControl;
import com.unboundid.ldap.sdk.unboundidds.controls.
            NameWithEntryUUIDRequestControl;
import com.unboundid.ldap.sdk.unboundidds.controls.NoOpRequestControl;
import com.unboundid.ldap.sdk.unboundidds.controls.
            OperationPurposeRequestControl;
import com.unboundid.ldap.sdk.unboundidds.controls.PasswordPolicyRequestControl;
import com.unboundid.ldap.sdk.unboundidds.controls.
            PasswordValidationDetailsRequestControl;
import com.unboundid.ldap.sdk.unboundidds.controls.PurgePasswordRequestControl;
import com.unboundid.ldap.sdk.unboundidds.controls.
            ReplicationRepairRequestControl;
import com.unboundid.ldap.sdk.unboundidds.controls.RetirePasswordRequestControl;
import com.unboundid.ldap.sdk.unboundidds.controls.SoftDeleteRequestControl;
import com.unboundid.ldap.sdk.unboundidds.controls.
            SuppressOperationalAttributeUpdateRequestControl;
import com.unboundid.ldap.sdk.unboundidds.controls.
            SuppressReferentialIntegrityUpdatesRequestControl;
import com.unboundid.ldap.sdk.unboundidds.controls.SuppressType;
import com.unboundid.ldap.sdk.unboundidds.controls.UndeleteRequestControl;
import com.unboundid.ldap.sdk.unboundidds.extensions.MultiUpdateErrorBehavior;
import com.unboundid.ldap.sdk.unboundidds.extensions.MultiUpdateExtendedRequest;
import com.unboundid.ldap.sdk.unboundidds.extensions.
            StartAdministrativeSessionExtendedRequest;
import com.unboundid.ldap.sdk.unboundidds.extensions.
            StartAdministrativeSessionPostConnectProcessor;
import com.unboundid.ldif.LDIFAddChangeRecord;
import com.unboundid.ldif.LDIFChangeRecord;
import com.unboundid.ldif.LDIFDeleteChangeRecord;
import com.unboundid.ldif.LDIFException;
import com.unboundid.ldif.LDIFModifyChangeRecord;
import com.unboundid.ldif.LDIFModifyDNChangeRecord;
import com.unboundid.ldif.LDIFReader;
import com.unboundid.ldif.LDIFWriter;
import com.unboundid.ldif.TrailingSpaceBehavior;
import com.unboundid.util.Debug;
import com.unboundid.util.DNFileReader;
import com.unboundid.util.FilterFileReader;
import com.unboundid.util.FixedRateBarrier;
import com.unboundid.util.LDAPCommandLineTool;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.BooleanArgument;
import com.unboundid.util.args.ControlArgument;
import com.unboundid.util.args.DNArgument;
import com.unboundid.util.args.DurationArgument;
import com.unboundid.util.args.FileArgument;
import com.unboundid.util.args.FilterArgument;
import com.unboundid.util.args.IntegerArgument;
import com.unboundid.util.args.StringArgument;

import static com.unboundid.ldap.sdk.unboundidds.tools.ToolMessages.*;



/**
 * This class provides an implementation of an LDAP command-line tool that may
 * be used to apply changes to a directory server.  The changes to apply (which
 * may include add, delete, modify, and modify DN operations) will be read in
 * LDIF form, either from standard input or a specified file or set of files.
 * This is a much more full-featured tool than the
 * {@link com.unboundid.ldap.sdk.examples.LDAPModify} tool
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
 */
@ThreadSafety(level=ThreadSafetyLevel.NOT_THREADSAFE)
public final class LDAPModify
       extends LDAPCommandLineTool
       implements UnsolicitedNotificationHandler
{
  /**
   * The column at which output should be wrapped.
   */
  private static final int WRAP_COLUMN = StaticUtils.TERMINAL_WIDTH_COLUMNS - 1;



  /**
   * The name of the attribute type used to specify a password in the
   * authentication password syntax as described in RFC 3112.
   */
  private static final String ATTR_AUTH_PASSWORD = "authPassword";



  /**
   * The name of the attribute type used to specify the DN of the soft-deleted
   * entry to be restored via an undelete operation.
   */
  private static final String ATTR_UNDELETE_FROM_DN = "ds-undelete-from-dn";



  /**
   * The name of the attribute type used to specify a password in the
   * userPassword syntax.
   */
  private static final String ATTR_USER_PASSWORD = "userPassword";



  /**
   * The long identifier for the argument used to specify the desired assured
   * replication local level.
   */
  private static final String ARG_ASSURED_REPLICATION_LOCAL_LEVEL =
       "assuredReplicationLocalLevel";



  /**
   * The long identifier for the argument used to specify the desired assured
   * replication remote level.
   */
  private static final String ARG_ASSURED_REPLICATION_REMOTE_LEVEL =
       "assuredReplicationRemoteLevel";



  /**
   * The long identifier for the argument used to specify the desired assured
   * timeout.
   */
  private static final String ARG_ASSURED_REPLICATION_TIMEOUT =
       "assuredReplicationTimeout";



  /**
   * The long identifier for the argument used to specify the path to an LDIF
   * file containing changes to apply.
   */
  private static final String ARG_LDIF_FILE = "ldifFile";



  /**
   * The long identifier for the argument used to specify the simple paged
   * results page size to use when modifying entries that match a provided
   * filter.
   */
  private static final String ARG_SEARCH_PAGE_SIZE = "searchPageSize";



  // The set of arguments supported by this program.
  private BooleanArgument allowUndelete = null;
  private BooleanArgument assuredReplication = null;
  private BooleanArgument authorizationIdentity = null;
  private BooleanArgument continueOnError = null;
  private BooleanArgument defaultAdd = null;
  private BooleanArgument dryRun = null;
  private BooleanArgument followReferrals = null;
  private BooleanArgument getUserResourceLimits = null;
  private BooleanArgument hardDelete = null;
  private BooleanArgument ignoreNoUserModification = null;
  private BooleanArgument manageDsaIT = null;
  private BooleanArgument nameWithEntryUUID = null;
  private BooleanArgument noOperation = null;
  private BooleanArgument passwordValidationDetails = null;
  private BooleanArgument permissiveModify = null;
  private BooleanArgument purgeCurrentPassword = null;
  private BooleanArgument replicationRepair = null;
  private BooleanArgument retireCurrentPassword = null;
  private BooleanArgument retryFailedOperations = null;
  private BooleanArgument softDelete = null;
  private BooleanArgument stripTrailingSpaces = null;
  private BooleanArgument subtreeDelete = null;
  private BooleanArgument suppressReferentialIntegrityUpdates = null;
  private BooleanArgument useAdministrativeSession = null;
  private BooleanArgument usePasswordPolicyControl = null;
  private BooleanArgument useTransaction = null;
  private BooleanArgument verbose = null;
  private ControlArgument addControl = null;
  private ControlArgument bindControl = null;
  private ControlArgument deleteControl = null;
  private ControlArgument modifyControl = null;
  private ControlArgument modifyDNControl = null;
  private ControlArgument operationControl = null;
  private DNArgument modifyEntryWithDN = null;
  private DNArgument proxyV1As = null;
  private DurationArgument assuredReplicationTimeout = null;
  private FileArgument ldifFile = null;
  private FileArgument modifyEntriesMatchingFiltersFromFile = null;
  private FileArgument modifyEntriesWithDNsFromFile = null;
  private FileArgument rejectFile = null;
  private FilterArgument assertionFilter = null;
  private FilterArgument modifyEntriesMatchingFilter = null;
  private IntegerArgument ratePerSecond = null;
  private IntegerArgument searchPageSize = null;
  private StringArgument assuredReplicationLocalLevel = null;
  private StringArgument assuredReplicationRemoteLevel = null;
  private StringArgument characterSet = null;
  private StringArgument getAuthorizationEntryAttribute = null;
  private StringArgument multiUpdateErrorBehavior = null;
  private StringArgument operationPurpose = null;
  private StringArgument postReadAttribute = null;
  private StringArgument preReadAttribute = null;
  private StringArgument proxyAs = null;
  private StringArgument suppressOperationalAttributeUpdates = null;

  // Indicates whether we've written anything to the reject writer yet.
  private final AtomicBoolean rejectWritten;

  // The input stream from to use for standard input.
  private final InputStream in;



  /**
   * Runs this tool with the provided command-line arguments.  It will use the
   * JVM-default streams for standard input, output, and error.
   *
   * @param  args  The command-line arguments to provide to this program.
   */
  public static void main(final String... args)
  {
    final ResultCode resultCode = main(System.in, System.out, System.err, args);
    if (resultCode != ResultCode.SUCCESS)
    {
      System.exit(Math.min(resultCode.intValue(), 255));
    }
  }



  /**
   * Runs this tool with the provided streams and command-line arguments.
   *
   * @param  in    The input stream to use for standard input.  If this is
   *               {@code null}, then no standard input will be used.
   * @param  out   The output stream to use for standard output.  If this is
   *               {@code null}, then standard output will be suppressed.
   * @param  err   The output stream to use for standard error.  If this is
   *               {@code null}, then standard error will be suppressed.
   * @param  args  The command-line arguments provided to this program.
   *
   * @return  The result code obtained when running the tool.  Any result code
   *          other than {@link ResultCode#SUCCESS} indicates an error.
   */
  public static ResultCode main(final InputStream in, final OutputStream out,
                                final OutputStream err, final String... args)
  {
    final LDAPModify tool = new LDAPModify(in, out, err);
    return tool.runTool(args);
  }



  /**
   * Creates a new instance of this tool with the provided streams.
   *
   * @param  in   The input stream to use for standard input.  If this is
   *              {@code null}, then no standard input will be used.
   * @param  out  The output stream to use for standard output.  If this is
   *              {@code null}, then standard output will be suppressed.
   * @param  err  The output stream to use for standard error.  If this is
   *              {@code null}, then standard error will be suppressed.
   */
  public LDAPModify(final InputStream in, final OutputStream out,
                    final OutputStream err)
  {
    super(out, err);

    if (in == null)
    {
      this.in = new ByteArrayInputStream(StaticUtils.NO_BYTES);
    }
    else
    {
      this.in = in;
    }


    rejectWritten = new AtomicBoolean(false);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getToolName()
  {
    return "ldapmodify";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getToolDescription()
  {
    return INFO_LDAPMODIFY_TOOL_DESCRIPTION.get(ARG_LDIF_FILE);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getToolVersion()
  {
    return Version.NUMERIC_VERSION_STRING;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public boolean supportsInteractiveMode()
  {
    return true;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public boolean defaultsToInteractiveMode()
  {
    return true;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public boolean supportsPropertiesFile()
  {
    return true;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public boolean supportsOutputFile()
  {
    return true;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  protected boolean defaultToPromptForBindPassword()
  {
    return true;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  protected boolean includeAlternateLongIdentifiers()
  {
    return true;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void addNonLDAPArguments(final ArgumentParser parser)
         throws ArgumentException
  {
    ldifFile = new FileArgument('f', ARG_LDIF_FILE, false, -1, null,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_LDIF_FILE.get(), true, true, true,
         false);
    ldifFile.addLongIdentifier("filename");
    ldifFile.addLongIdentifier("ldif-file");
    ldifFile.setArgumentGroupName(INFO_LDAPMODIFY_ARG_GROUP_DATA.get());
    parser.addArgument(ldifFile);


    characterSet = new StringArgument('i', "characterSet", false, 1,
         INFO_LDAPMODIFY_PLACEHOLDER_CHARSET.get(),
         INFO_LDAPMODIFY_ARG_DESCRIPTION_CHARACTER_SET.get(), "UTF-8");
    characterSet.addLongIdentifier("encoding");
    characterSet.addLongIdentifier("character-set");
    characterSet.setArgumentGroupName(INFO_LDAPMODIFY_ARG_GROUP_DATA.get());
    parser.addArgument(characterSet);


    rejectFile = new FileArgument('R', "rejectFile", false, 1, null,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_REJECT_FILE.get(), false, true, true,
         false);
    rejectFile.addLongIdentifier("reject-file");
    rejectFile.setArgumentGroupName(INFO_LDAPMODIFY_ARG_GROUP_DATA.get());
    parser.addArgument(rejectFile);


    verbose = new BooleanArgument('v', "verbose", 1,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_VERBOSE.get());
    verbose.setArgumentGroupName(INFO_LDAPMODIFY_ARG_GROUP_DATA.get());
    parser.addArgument(verbose);


    modifyEntriesMatchingFilter = new FilterArgument(null,
         "modifyEntriesMatchingFilter", false, 0, null,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_MODIFY_ENTRIES_MATCHING_FILTER.get(
              ARG_SEARCH_PAGE_SIZE));
    modifyEntriesMatchingFilter.addLongIdentifier(
         "modify-entries-matching-filter");
    modifyEntriesMatchingFilter.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_DATA.get());
    parser.addArgument(modifyEntriesMatchingFilter);


    modifyEntriesMatchingFiltersFromFile = new FileArgument(null,
         "modifyEntriesMatchingFiltersFromFile", false, 0, null,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_MODIFY_FILTER_FILE.get(
              ARG_SEARCH_PAGE_SIZE), true, false, true, false);
    modifyEntriesMatchingFiltersFromFile.addLongIdentifier(
         "modify-entries-matching-filters-from-file");
    modifyEntriesMatchingFiltersFromFile.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_DATA.get());
    parser.addArgument(modifyEntriesMatchingFiltersFromFile);


    modifyEntryWithDN = new DNArgument(null, "modifyEntryWithDN", false, 0,
         null, INFO_LDAPMODIFY_ARG_DESCRIPTION_MODIFY_ENTRY_DN.get());
    modifyEntryWithDN.addLongIdentifier("modify-entry-with-dn");
    modifyEntryWithDN.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_DATA.get());
    parser.addArgument(modifyEntryWithDN);


    modifyEntriesWithDNsFromFile = new FileArgument(null,
         "modifyEntriesWithDNsFromFile", false, 0,
         null, INFO_LDAPMODIFY_ARG_DESCRIPTION_MODIFY_DN_FILE.get(), true,
         false, true, false);
    modifyEntriesWithDNsFromFile.addLongIdentifier(
         "modify-entries-with-dns-from-file");
    modifyEntriesWithDNsFromFile.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_DATA.get());
    parser.addArgument(modifyEntriesWithDNsFromFile);


    searchPageSize = new IntegerArgument(null, ARG_SEARCH_PAGE_SIZE, false, 1,
         null,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_SEARCH_PAGE_SIZE.get(
              modifyEntriesMatchingFilter.getIdentifierString(),
              modifyEntriesMatchingFiltersFromFile.getIdentifierString()),
         1, Integer.MAX_VALUE);
    searchPageSize.addLongIdentifier("search-page-size");
    searchPageSize.setArgumentGroupName(INFO_LDAPMODIFY_ARG_GROUP_DATA.get());
    parser.addArgument(searchPageSize);


    retryFailedOperations = new BooleanArgument(null, "retryFailedOperations",
         1, INFO_LDAPMODIFY_ARG_DESCRIPTION_RETRY_FAILED_OPERATIONS.get());
    retryFailedOperations.addLongIdentifier("retry-failed-operations");
    retryFailedOperations.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_OPS.get());
    parser.addArgument(retryFailedOperations);


    dryRun = new BooleanArgument('n', "dryRun", 1,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_DRY_RUN.get());
    dryRun.addLongIdentifier("dry-run");
    dryRun.setArgumentGroupName(INFO_LDAPMODIFY_ARG_GROUP_OPS.get());
    parser.addArgument(dryRun);


    defaultAdd = new BooleanArgument('a', "defaultAdd", 1,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_DEFAULT_ADD.get());
    defaultAdd.addLongIdentifier("default-add");
    defaultAdd.setArgumentGroupName(INFO_LDAPMODIFY_ARG_GROUP_OPS.get());
    parser.addArgument(defaultAdd);


    continueOnError = new BooleanArgument('c', "continueOnError", 1,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_CONTINUE_ON_ERROR.get());
    continueOnError.addLongIdentifier("continue-on-error");
    continueOnError.setArgumentGroupName(INFO_LDAPMODIFY_ARG_GROUP_OPS.get());
    parser.addArgument(continueOnError);


    stripTrailingSpaces = new BooleanArgument(null, "stripTrailingSpaces", 1,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_STRIP_TRAILING_SPACES.get());
    stripTrailingSpaces.addLongIdentifier("strip-trailing-spaces");
    stripTrailingSpaces.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_DATA.get());
    parser.addArgument(stripTrailingSpaces);



    followReferrals = new BooleanArgument(null, "followReferrals", 1,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_FOLLOW_REFERRALS.get());
    followReferrals.addLongIdentifier("follow-referrals");
    followReferrals.setArgumentGroupName(INFO_LDAPMODIFY_ARG_GROUP_OPS.get());
    parser.addArgument(followReferrals);


    proxyAs = new StringArgument('Y', "proxyAs", false, 1,
         INFO_PLACEHOLDER_AUTHZID.get(),
         INFO_LDAPMODIFY_ARG_DESCRIPTION_PROXY_AS.get());
    proxyAs.addLongIdentifier("proxyV2As");
    proxyAs.addLongIdentifier("proxy-as");
    proxyAs.addLongIdentifier("proxy-v2-as");
    proxyAs.setArgumentGroupName(INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(proxyAs);

    proxyV1As = new DNArgument(null, "proxyV1As", false, 1, null,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_PROXY_V1_AS.get());
    proxyV1As.addLongIdentifier("proxy-v1-as");
    proxyV1As.setArgumentGroupName(INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(proxyV1As);


    useAdministrativeSession = new BooleanArgument(null,
         "useAdministrativeSession", 1,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_USE_ADMIN_SESSION.get());
    useAdministrativeSession.addLongIdentifier("use-administrative-session");
    useAdministrativeSession.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_OPS.get());
    parser.addArgument(useAdministrativeSession);


    operationPurpose = new StringArgument(null, "operationPurpose", false, 1,
         INFO_PLACEHOLDER_PURPOSE.get(),
         INFO_LDAPMODIFY_ARG_DESCRIPTION_OPERATION_PURPOSE.get());
    operationPurpose.addLongIdentifier("operation-purpose");
    operationPurpose.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(operationPurpose);


    manageDsaIT = new BooleanArgument(null, "useManageDsaIT", 1,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_MANAGE_DSA_IT.get());
    manageDsaIT.addLongIdentifier("manageDsaIT");
    manageDsaIT.addLongIdentifier("use-manage-dsa-it");
    manageDsaIT.addLongIdentifier("manage-dsa-it");
    manageDsaIT.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(manageDsaIT);


    useTransaction = new BooleanArgument(null, "useTransaction", 1,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_USE_TRANSACTION.get());
    useTransaction.addLongIdentifier("use-transaction");
    useTransaction.setArgumentGroupName(INFO_LDAPMODIFY_ARG_GROUP_OPS.get());
    parser.addArgument(useTransaction);


    final LinkedHashSet<String> multiUpdateErrorBehaviorAllowedValues =
         new LinkedHashSet<String>(3);
    multiUpdateErrorBehaviorAllowedValues.add("atomic");
    multiUpdateErrorBehaviorAllowedValues.add("abort-on-error");
    multiUpdateErrorBehaviorAllowedValues.add("continue-on-error");
    multiUpdateErrorBehavior = new StringArgument(null,
         "multiUpdateErrorBehavior", false, 1,
         "{atomic|abort-on-error|continue-on-error}",
         INFO_LDAPMODIFY_ARG_DESCRIPTION_MULTI_UPDATE_ERROR_BEHAVIOR.get(),
         multiUpdateErrorBehaviorAllowedValues);
    multiUpdateErrorBehavior.addLongIdentifier("multi-update-error-behavior");
    multiUpdateErrorBehavior.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_OPS.get());
    parser.addArgument(multiUpdateErrorBehavior);


    assertionFilter = new FilterArgument(null, "assertionFilter", false, 1,
         INFO_PLACEHOLDER_FILTER.get(),
         INFO_LDAPMODIFY_ARG_DESCRIPTION_ASSERTION_FILTER.get());
    assertionFilter.addLongIdentifier("assertion-filter");
    assertionFilter.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(assertionFilter);


    authorizationIdentity = new BooleanArgument('E',
         "authorizationIdentity", 1,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_AUTHZ_IDENTITY.get());
    authorizationIdentity.addLongIdentifier("reportAuthzID");
    authorizationIdentity.addLongIdentifier("authorization-identity");
    authorizationIdentity.addLongIdentifier("report-authzID");
    authorizationIdentity.addLongIdentifier("report-authz-id");
    authorizationIdentity.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(authorizationIdentity);


    getAuthorizationEntryAttribute = new StringArgument(null,
         "getAuthorizationEntryAttribute", false, 0,
         INFO_PLACEHOLDER_ATTR.get(),
         INFO_LDAPMODIFY_ARG_DESCRIPTION_GET_AUTHZ_ENTRY_ATTR.get());
    getAuthorizationEntryAttribute.addLongIdentifier(
         "get-authorization-entry-attribute");
    getAuthorizationEntryAttribute.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(getAuthorizationEntryAttribute);


    getUserResourceLimits = new BooleanArgument(null, "getUserResourceLimits",
         1, INFO_LDAPMODIFY_ARG_DESCRIPTION_GET_USER_RESOURCE_LIMITS.get());
    getUserResourceLimits.addLongIdentifier("get-user-resource-limits");
    getUserResourceLimits.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(getUserResourceLimits);


    ignoreNoUserModification = new BooleanArgument(null,
         "ignoreNoUserModification", 1,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_IGNORE_NO_USER_MOD.get());
    ignoreNoUserModification.addLongIdentifier("ignore-no-user-modification");
    ignoreNoUserModification.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(ignoreNoUserModification);


    preReadAttribute = new StringArgument(null, "preReadAttribute", false, -1,
         INFO_PLACEHOLDER_ATTR.get(),
         INFO_LDAPMODIFY_ARG_DESCRIPTION_PRE_READ_ATTRIBUTE.get());
    preReadAttribute.addLongIdentifier("preReadAttributes");
    preReadAttribute.addLongIdentifier("pre-read-attribute");
    preReadAttribute.addLongIdentifier("pre-read-attributes");
    preReadAttribute.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(preReadAttribute);


    postReadAttribute = new StringArgument(null, "postReadAttribute", false,
         -1, INFO_PLACEHOLDER_ATTR.get(),
         INFO_LDAPMODIFY_ARG_DESCRIPTION_POST_READ_ATTRIBUTE.get());
    postReadAttribute.addLongIdentifier("postReadAttributes");
    postReadAttribute.addLongIdentifier("post-read-attribute");
    postReadAttribute.addLongIdentifier("post-read-attributes");
    postReadAttribute.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(postReadAttribute);


    assuredReplication = new BooleanArgument(null, "useAssuredReplication", 1,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_ASSURED_REPLICATION.get(
              ARG_ASSURED_REPLICATION_LOCAL_LEVEL,
              ARG_ASSURED_REPLICATION_REMOTE_LEVEL,
              ARG_ASSURED_REPLICATION_TIMEOUT));
    assuredReplication.addLongIdentifier("assuredReplication");
    assuredReplication.addLongIdentifier("use-assured-replication");
    assuredReplication.addLongIdentifier("assured-replication");
    assuredReplication.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(assuredReplication);


    final LinkedHashSet<String> assuredReplicationLocalLevelAllowedValues =
         new LinkedHashSet<String>(3);
    assuredReplicationLocalLevelAllowedValues.add("none");
    assuredReplicationLocalLevelAllowedValues.add("received-any-server");
    assuredReplicationLocalLevelAllowedValues.add("processed-all-servers");
    assuredReplicationLocalLevel = new StringArgument(null,
         ARG_ASSURED_REPLICATION_LOCAL_LEVEL, false, 1,
         INFO_PLACEHOLDER_LEVEL.get(),
         INFO_LDAPMODIFY_ARG_DESCRIPTION_ASSURED_REPL_LOCAL_LEVEL.get(
              assuredReplication.getIdentifierString()),
         assuredReplicationLocalLevelAllowedValues);
    assuredReplicationLocalLevel.addLongIdentifier(
         "assured-replication-local-level");
    assuredReplicationLocalLevel.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(assuredReplicationLocalLevel);


    final LinkedHashSet<String> assuredReplicationRemoteLevelAllowedValues =
         new LinkedHashSet<String>(4);
    assuredReplicationRemoteLevelAllowedValues.add("none");
    assuredReplicationRemoteLevelAllowedValues.add(
         "received-any-remote-location");
    assuredReplicationRemoteLevelAllowedValues.add(
         "received-all-remote-locations");
    assuredReplicationRemoteLevelAllowedValues.add(
         "processed-all-remote-servers");
    assuredReplicationRemoteLevel = new StringArgument(null,
         ARG_ASSURED_REPLICATION_REMOTE_LEVEL, false, 1,
         INFO_PLACEHOLDER_LEVEL.get(),
         INFO_LDAPMODIFY_ARG_DESCRIPTION_ASSURED_REPL_REMOTE_LEVEL.get(
              assuredReplication.getIdentifierString()),
         assuredReplicationRemoteLevelAllowedValues);
    assuredReplicationRemoteLevel.addLongIdentifier(
         "assured-replication-remote-level");
    assuredReplicationRemoteLevel.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(assuredReplicationRemoteLevel);


    assuredReplicationTimeout = new DurationArgument(null,
         ARG_ASSURED_REPLICATION_TIMEOUT, false, INFO_PLACEHOLDER_TIMEOUT.get(),
         INFO_LDAPMODIFY_ARG_DESCRIPTION_ASSURED_REPL_TIMEOUT.get(
              assuredReplication.getIdentifierString()));
    assuredReplicationTimeout.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(assuredReplicationTimeout);


    replicationRepair = new BooleanArgument(null, "replicationRepair",
         1, INFO_LDAPMODIFY_ARG_DESCRIPTION_REPLICATION_REPAIR.get());
    replicationRepair.addLongIdentifier("replication-repair");
    replicationRepair.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(replicationRepair);


    nameWithEntryUUID = new BooleanArgument(null, "nameWithEntryUUID", 1,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_NAME_WITH_ENTRY_UUID.get());
    nameWithEntryUUID.addLongIdentifier("name-with-entryUUID");
    nameWithEntryUUID.addLongIdentifier("name-with-entry-uuid");
    nameWithEntryUUID.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(nameWithEntryUUID);


    noOperation = new BooleanArgument(null, "noOperation", 1,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_NO_OPERATION.get());
    noOperation.addLongIdentifier("noOp");
    noOperation.addLongIdentifier("no-operation");
    noOperation.addLongIdentifier("no-op");
    noOperation.setArgumentGroupName(INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(noOperation);


    passwordValidationDetails = new BooleanArgument(null,
         "getPasswordValidationDetails", 1,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_PASSWORD_VALIDATION_DETAILS.get(
              ATTR_USER_PASSWORD, ATTR_AUTH_PASSWORD));
    passwordValidationDetails.addLongIdentifier("passwordValidationDetails");
    passwordValidationDetails.addLongIdentifier(
         "get-password-validation-details");
    passwordValidationDetails.addLongIdentifier("password-validation-details");
    passwordValidationDetails.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(passwordValidationDetails);


    permissiveModify = new BooleanArgument(null, "permissiveModify",
         1, INFO_LDAPMODIFY_ARG_DESCRIPTION_PERMISSIVE_MODIFY.get());
    permissiveModify.addLongIdentifier("permissive-modify");
    permissiveModify.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(permissiveModify);


    subtreeDelete = new BooleanArgument(null, "subtreeDelete", 1,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_SUBTREE_DELETE.get());
    subtreeDelete.addLongIdentifier("subtree-delete");
    subtreeDelete.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(subtreeDelete);


    softDelete = new BooleanArgument('s', "softDelete", 1,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_SOFT_DELETE.get());
    softDelete.addLongIdentifier("useSoftDelete");
    softDelete.addLongIdentifier("soft-delete");
    softDelete.addLongIdentifier("use-soft-delete");
    softDelete.setArgumentGroupName(INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(softDelete);


    hardDelete = new BooleanArgument(null, "hardDelete", 1,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_HARD_DELETE.get());
    hardDelete.addLongIdentifier("hard-delete");
    hardDelete.setArgumentGroupName(INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(hardDelete);


    allowUndelete = new BooleanArgument(null, "allowUndelete", 1,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_ALLOW_UNDELETE.get(
              ATTR_UNDELETE_FROM_DN));
    allowUndelete.addLongIdentifier("allow-undelete");
    allowUndelete.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(allowUndelete);


    retireCurrentPassword = new BooleanArgument(null, "retireCurrentPassword",
         1,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_RETIRE_CURRENT_PASSWORD.get(
              ATTR_USER_PASSWORD, ATTR_AUTH_PASSWORD));
    retireCurrentPassword.addLongIdentifier("retire-current-password");
    retireCurrentPassword.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(retireCurrentPassword);


    purgeCurrentPassword = new BooleanArgument(null, "purgeCurrentPassword", 1,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_PURGE_CURRENT_PASSWORD.get(
              ATTR_USER_PASSWORD, ATTR_AUTH_PASSWORD));
    purgeCurrentPassword.addLongIdentifier("purge-current-password");
    purgeCurrentPassword.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(purgeCurrentPassword);


    final LinkedHashSet<String>
         suppressOperationalAttributeUpdatesAllowedValues =
              new LinkedHashSet<String>(4);
    suppressOperationalAttributeUpdatesAllowedValues.add("last-access-time");
    suppressOperationalAttributeUpdatesAllowedValues.add("last-login-time");
    suppressOperationalAttributeUpdatesAllowedValues.add("last-login-ip");
    suppressOperationalAttributeUpdatesAllowedValues.add("lastmod");
    suppressOperationalAttributeUpdates = new StringArgument(null,
         "suppressOperationalAttributeUpdates", false, -1,
         INFO_PLACEHOLDER_ATTR.get(),
         INFO_LDAPMODIFY_ARG_DESCRIPTION_SUPPRESS_OP_ATTR_UPDATES.get(),
         suppressOperationalAttributeUpdatesAllowedValues);
    suppressOperationalAttributeUpdates.addLongIdentifier(
         "suppress-operational-attribute-updates");
    suppressOperationalAttributeUpdates.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(suppressOperationalAttributeUpdates);


    suppressReferentialIntegrityUpdates = new BooleanArgument(null,
         "suppressReferentialIntegrityUpdates", 1,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_SUPPRESS_REFERINT_UPDATES.get());
    suppressReferentialIntegrityUpdates.addLongIdentifier(
         "suppress-referential-integrity-updates");
    suppressReferentialIntegrityUpdates.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(suppressReferentialIntegrityUpdates);


    usePasswordPolicyControl = new BooleanArgument(null,
         "usePasswordPolicyControl", 1,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_PASSWORD_POLICY.get());
    usePasswordPolicyControl.addLongIdentifier("use-password-policy-control");
    usePasswordPolicyControl.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(usePasswordPolicyControl);


    operationControl = new ControlArgument('J', "control", false, 0, null,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_OP_CONTROL.get());
    operationControl.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(operationControl);


    addControl = new ControlArgument(null, "addControl", false, 0, null,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_ADD_CONTROL.get());
    addControl.addLongIdentifier("add-control");
    addControl.setArgumentGroupName(INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(addControl);


    bindControl = new ControlArgument(null, "bindControl", false, 0, null,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_BIND_CONTROL.get());
    bindControl.addLongIdentifier("bind-control");
    bindControl.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(bindControl);


    deleteControl = new ControlArgument(null, "deleteControl", false, 0, null,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_DELETE_CONTROL.get());
    deleteControl.addLongIdentifier("delete-control");
    deleteControl.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(deleteControl);


    modifyControl = new ControlArgument(null, "modifyControl", false, 0, null,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_MODIFY_CONTROL.get());
    modifyControl.addLongIdentifier("modify-control");
    modifyControl.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(modifyControl);


    modifyDNControl = new ControlArgument(null, "modifyDNControl", false, 0,
         null, INFO_LDAPMODIFY_ARG_DESCRIPTION_MODIFY_DN_CONTROL.get());
    modifyDNControl.addLongIdentifier("modify-dn-control");
    modifyDNControl.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_CONTROLS.get());
    parser.addArgument(modifyDNControl);


    ratePerSecond = new IntegerArgument('r', "ratePerSecond", false, 1,
         INFO_PLACEHOLDER_NUM.get(),
         INFO_LDAPMODIFY_ARG_DESCRIPTION_RATE_PER_SECOND.get(), 1,
         Integer.MAX_VALUE);
    ratePerSecond.addLongIdentifier("rate-per-second");
    ratePerSecond.setArgumentGroupName(INFO_LDAPMODIFY_ARG_GROUP_OPS.get());
    parser.addArgument(ratePerSecond);


    // The "--scriptFriendly" argument is provided for compatibility with legacy
    // ldapmodify tools, but is not actually used by this tool.
    final BooleanArgument scriptFriendly = new BooleanArgument(null,
         "scriptFriendly", 1,
         INFO_LDAPMODIFY_ARG_DESCRIPTION_SCRIPT_FRIENDLY.get());
    scriptFriendly.addLongIdentifier("script-friendly");
    scriptFriendly.setArgumentGroupName(
         INFO_LDAPMODIFY_ARG_GROUP_DATA.get());
    scriptFriendly.setHidden(true);
    parser.addArgument(scriptFriendly);


    // The "-V" / "--ldapVersion" argument is provided for compatibility with
    // legacy ldapmodify tools, but is not actually used by this tool.
    final IntegerArgument ldapVersion = new IntegerArgument('V', "ldapVersion",
         false, 1, null, INFO_LDAPMODIFY_ARG_DESCRIPTION_LDAP_VERSION.get());
    ldapVersion.addLongIdentifier("ldap-version");
    ldapVersion.setHidden(true);
    parser.addArgument(ldapVersion);


    // A few assured replication arguments will only be allowed if assured
    // replication is to be used.
    parser.addDependentArgumentSet(assuredReplicationLocalLevel,
         assuredReplication);
    parser.addDependentArgumentSet(assuredReplicationRemoteLevel,
         assuredReplication);
    parser.addDependentArgumentSet(assuredReplicationTimeout,
         assuredReplication);

    // Transactions will be incompatible with a lot of settings.
    parser.addExclusiveArgumentSet(useTransaction, multiUpdateErrorBehavior);
    parser.addExclusiveArgumentSet(useTransaction, rejectFile);
    parser.addExclusiveArgumentSet(useTransaction, retryFailedOperations);
    parser.addExclusiveArgumentSet(useTransaction, continueOnError);
    parser.addExclusiveArgumentSet(useTransaction, dryRun);
    parser.addExclusiveArgumentSet(useTransaction, followReferrals);
    parser.addExclusiveArgumentSet(useTransaction, nameWithEntryUUID);
    parser.addExclusiveArgumentSet(useTransaction, noOperation);
    parser.addExclusiveArgumentSet(useTransaction, operationControl);
    parser.addExclusiveArgumentSet(useTransaction, addControl);
    parser.addExclusiveArgumentSet(useTransaction, deleteControl);
    parser.addExclusiveArgumentSet(useTransaction, modifyControl);
    parser.addExclusiveArgumentSet(useTransaction, modifyDNControl);
    parser.addExclusiveArgumentSet(useTransaction, modifyEntriesMatchingFilter);
    parser.addExclusiveArgumentSet(useTransaction,
         modifyEntriesMatchingFiltersFromFile);
    parser.addExclusiveArgumentSet(useTransaction, modifyEntryWithDN);
    parser.addExclusiveArgumentSet(useTransaction,
         modifyEntriesWithDNsFromFile);

    // Multi-update is incompatible with a lot of settings.
    parser.addExclusiveArgumentSet(multiUpdateErrorBehavior, ratePerSecond);
    parser.addExclusiveArgumentSet(multiUpdateErrorBehavior, rejectFile);
    parser.addExclusiveArgumentSet(multiUpdateErrorBehavior,
         retryFailedOperations);
    parser.addExclusiveArgumentSet(multiUpdateErrorBehavior, continueOnError);
    parser.addExclusiveArgumentSet(multiUpdateErrorBehavior, dryRun);
    parser.addExclusiveArgumentSet(multiUpdateErrorBehavior, followReferrals);
    parser.addExclusiveArgumentSet(multiUpdateErrorBehavior, nameWithEntryUUID);
    parser.addExclusiveArgumentSet(multiUpdateErrorBehavior, noOperation);
    parser.addExclusiveArgumentSet(multiUpdateErrorBehavior, operationControl);
    parser.addExclusiveArgumentSet(multiUpdateErrorBehavior, addControl);
    parser.addExclusiveArgumentSet(multiUpdateErrorBehavior, deleteControl);
    parser.addExclusiveArgumentSet(multiUpdateErrorBehavior, modifyControl);
    parser.addExclusiveArgumentSet(multiUpdateErrorBehavior, modifyDNControl);
    parser.addExclusiveArgumentSet(multiUpdateErrorBehavior,
         modifyEntriesMatchingFilter);
    parser.addExclusiveArgumentSet(multiUpdateErrorBehavior,
         modifyEntriesMatchingFiltersFromFile);
    parser.addExclusiveArgumentSet(multiUpdateErrorBehavior, modifyEntryWithDN);
    parser.addExclusiveArgumentSet(multiUpdateErrorBehavior,
         modifyEntriesWithDNsFromFile);

    // Soft delete cannot be used with either hard delete or subtree delete.
    parser.addExclusiveArgumentSet(softDelete, hardDelete);
    parser.addExclusiveArgumentSet(softDelete, subtreeDelete);

    // Password retiring and purging can't be used together.
    parser.addExclusiveArgumentSet(retireCurrentPassword, purgeCurrentPassword);

    // Referral following cannot be used in conjunction with the manageDsaIT
    // control.
    parser.addExclusiveArgumentSet(followReferrals, manageDsaIT);

    // The proxyAs and proxyV1As arguments cannot be used together.
    parser.addExclusiveArgumentSet(proxyAs, proxyV1As);

    // The modifyEntriesMatchingFilter argument is incompatible with a lot of
    // settings, since it can only be used for modify operations.
    parser.addExclusiveArgumentSet(modifyEntriesMatchingFilter, allowUndelete);
    parser.addExclusiveArgumentSet(modifyEntriesMatchingFilter, defaultAdd);
    parser.addExclusiveArgumentSet(modifyEntriesMatchingFilter, dryRun);
    parser.addExclusiveArgumentSet(modifyEntriesMatchingFilter, hardDelete);
    parser.addExclusiveArgumentSet(modifyEntriesMatchingFilter,
         ignoreNoUserModification);
    parser.addExclusiveArgumentSet(modifyEntriesMatchingFilter,
         nameWithEntryUUID);
    parser.addExclusiveArgumentSet(modifyEntriesMatchingFilter, softDelete);
    parser.addExclusiveArgumentSet(modifyEntriesMatchingFilter, subtreeDelete);
    parser.addExclusiveArgumentSet(modifyEntriesMatchingFilter,
         suppressReferentialIntegrityUpdates);
    parser.addExclusiveArgumentSet(modifyEntriesMatchingFilter, addControl);
    parser.addExclusiveArgumentSet(modifyEntriesMatchingFilter, deleteControl);
    parser.addExclusiveArgumentSet(modifyEntriesMatchingFilter,
         modifyDNControl);

    // The modifyEntriesMatchingFilterFromFile argument is incompatible with a
    // lot of settings, since it can only be used for modify operations.
    parser.addExclusiveArgumentSet(modifyEntriesMatchingFiltersFromFile,
         allowUndelete);
    parser.addExclusiveArgumentSet(modifyEntriesMatchingFiltersFromFile,
         defaultAdd);
    parser.addExclusiveArgumentSet(modifyEntriesMatchingFiltersFromFile,
         dryRun);
    parser.addExclusiveArgumentSet(modifyEntriesMatchingFiltersFromFile,
         hardDelete);
    parser.addExclusiveArgumentSet(modifyEntriesMatchingFiltersFromFile,
         ignoreNoUserModification);
    parser.addExclusiveArgumentSet(modifyEntriesMatchingFiltersFromFile,
         nameWithEntryUUID);
    parser.addExclusiveArgumentSet(modifyEntriesMatchingFiltersFromFile,
         softDelete);
    parser.addExclusiveArgumentSet(modifyEntriesMatchingFiltersFromFile,
         subtreeDelete);
    parser.addExclusiveArgumentSet(modifyEntriesMatchingFiltersFromFile,
         suppressReferentialIntegrityUpdates);
    parser.addExclusiveArgumentSet(modifyEntriesMatchingFiltersFromFile,
         addControl);
    parser.addExclusiveArgumentSet(modifyEntriesMatchingFiltersFromFile,
         deleteControl);
    parser.addExclusiveArgumentSet(modifyEntriesMatchingFiltersFromFile,
         modifyDNControl);

    // The modifyEntryWithDN argument is incompatible with a lot of
    // settings, since it can only be used for modify operations.
    parser.addExclusiveArgumentSet(modifyEntryWithDN, allowUndelete);
    parser.addExclusiveArgumentSet(modifyEntryWithDN, defaultAdd);
    parser.addExclusiveArgumentSet(modifyEntryWithDN, dryRun);
    parser.addExclusiveArgumentSet(modifyEntryWithDN, hardDelete);
    parser.addExclusiveArgumentSet(modifyEntryWithDN, ignoreNoUserModification);
    parser.addExclusiveArgumentSet(modifyEntryWithDN, nameWithEntryUUID);
    parser.addExclusiveArgumentSet(modifyEntryWithDN, softDelete);
    parser.addExclusiveArgumentSet(modifyEntryWithDN, subtreeDelete);
    parser.addExclusiveArgumentSet(modifyEntryWithDN,
         suppressReferentialIntegrityUpdates);
    parser.addExclusiveArgumentSet(modifyEntryWithDN, addControl);
    parser.addExclusiveArgumentSet(modifyEntryWithDN, deleteControl);
    parser.addExclusiveArgumentSet(modifyEntryWithDN, modifyDNControl);

    // The modifyEntriesWithDNsFromFile argument is incompatible with a lot of
    // settings, since it can only be used for modify operations.
    parser.addExclusiveArgumentSet(modifyEntriesWithDNsFromFile, allowUndelete);
    parser.addExclusiveArgumentSet(modifyEntriesWithDNsFromFile, defaultAdd);
    parser.addExclusiveArgumentSet(modifyEntriesWithDNsFromFile, dryRun);
    parser.addExclusiveArgumentSet(modifyEntriesWithDNsFromFile, hardDelete);
    parser.addExclusiveArgumentSet(modifyEntriesWithDNsFromFile,
         ignoreNoUserModification);
    parser.addExclusiveArgumentSet(modifyEntriesWithDNsFromFile,
         nameWithEntryUUID);
    parser.addExclusiveArgumentSet(modifyEntriesWithDNsFromFile, softDelete);
    parser.addExclusiveArgumentSet(modifyEntriesWithDNsFromFile, subtreeDelete);
    parser.addExclusiveArgumentSet(modifyEntriesWithDNsFromFile,
         suppressReferentialIntegrityUpdates);
    parser.addExclusiveArgumentSet(modifyEntriesWithDNsFromFile, addControl);
    parser.addExclusiveArgumentSet(modifyEntriesWithDNsFromFile, deleteControl);
    parser.addExclusiveArgumentSet(modifyEntriesWithDNsFromFile,
         modifyDNControl);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  protected List<Control> getBindControls()
  {
    final ArrayList<Control> bindControls = new ArrayList<Control>(10);

    if (bindControl.isPresent())
    {
      bindControls.addAll(bindControl.getValues());
    }

    if (authorizationIdentity.isPresent())
    {
      bindControls.add(new AuthorizationIdentityRequestControl(false));
    }

    if (getAuthorizationEntryAttribute.isPresent())
    {
      bindControls.add(new GetAuthorizationEntryRequestControl(true, true,
           getAuthorizationEntryAttribute.getValues()));
    }

    if (getUserResourceLimits.isPresent())
    {
      bindControls.add(new GetUserResourceLimitsRequestControl());
    }

    if (usePasswordPolicyControl.isPresent())
    {
      bindControls.add(new PasswordPolicyRequestControl());
    }

    if (suppressOperationalAttributeUpdates.isPresent())
    {
      final EnumSet<SuppressType> suppressTypes =
           EnumSet.noneOf(SuppressType.class);
      for (final String s : suppressOperationalAttributeUpdates.getValues())
      {
        if (s.equalsIgnoreCase("last-access-time"))
        {
          suppressTypes.add(SuppressType.LAST_ACCESS_TIME);
        }
        else if (s.equalsIgnoreCase("last-login-time"))
        {
          suppressTypes.add(SuppressType.LAST_LOGIN_TIME);
        }
        else if (s.equalsIgnoreCase("last-login-ip"))
        {
          suppressTypes.add(SuppressType.LAST_LOGIN_IP);
        }
      }

      bindControls.add(new SuppressOperationalAttributeUpdateRequestControl(
           suppressTypes));
    }

    return bindControls;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  protected boolean supportsMultipleServers()
  {
    // We will support providing information about multiple servers.  This tool
    // will not communicate with multiple servers concurrently, but it can
    // accept information about multiple servers in the event that a large set
    // of changes is to be processed and a server goes down in the middle of
    // those changes.  In this case, we can resume processing on a newly-created
    // connection, possibly to a different server.
    return true;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public LDAPConnectionOptions getConnectionOptions()
  {
    final LDAPConnectionOptions options = new LDAPConnectionOptions();

    options.setUseSynchronousMode(true);
    options.setFollowReferrals(followReferrals.isPresent());
    options.setUnsolicitedNotificationHandler(this);

    return options;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public ResultCode doToolProcessing()
  {
    // Examine the arguments to determine the sets of controls to use for each
    // type of request.
    final ArrayList<Control> addControls = new ArrayList<Control>(10);
    final ArrayList<Control> deleteControls = new ArrayList<Control>(10);
    final ArrayList<Control> modifyControls = new ArrayList<Control>(10);
    final ArrayList<Control> modifyDNControls = new ArrayList<Control>(10);
    final ArrayList<Control> searchControls = new ArrayList<Control>(10);
    createRequestControls(addControls, deleteControls, modifyControls,
         modifyDNControls, searchControls);


    LDAPConnectionPool connectionPool = null;
    LDIFReader         ldifReader     = null;
    LDIFWriter         rejectWriter   = null;
    try
    {
      // Create a connection pool that will be used to communicate with the
      // directory server.  If we should use an administrative session, then
      // create a connect processor that will be used to start the session
      // before performing the bind.
      try
      {
        final StartAdministrativeSessionPostConnectProcessor p;
        if (useAdministrativeSession.isPresent())
        {
          p = new StartAdministrativeSessionPostConnectProcessor(
               new StartAdministrativeSessionExtendedRequest(getToolName(),
                    true));
        }
        else
        {
          p = null;
        }

        if (! dryRun.isPresent())
        {
          connectionPool = getConnectionPool(1, 2, 0, p, null, true,
               new ReportBindResultLDAPConnectionPoolHealthCheck(this, true,
                    verbose.isPresent()));
        }
      }
      catch (final LDAPException le)
      {
        Debug.debugException(le);

        // Unable to create the connection pool, which means that either the
        // connection could not be established or the attempt to authenticate
        // the connection failed.  If the bind failed, then the report bind
        // result health check should have already reported the bind failure.
        // If the failure was something else, then display that failure result.
        if (le.getResultCode() != ResultCode.INVALID_CREDENTIALS)
        {
          for (final String line :
               ResultUtils.formatResult(le, true, 0, WRAP_COLUMN))
          {
            err(line);
          }
        }
        return le.getResultCode();
      }

      if ((connectionPool != null) && retryFailedOperations.isPresent())
      {
        connectionPool.setRetryFailedOperationsDueToInvalidConnections(true);
      }


      // Report that the connection was successfully established.
      if (connectionPool != null)
      {
        try
        {
          final LDAPConnection connection = connectionPool.getConnection();
          final String hostPort = connection.getHostPort();
          connectionPool.releaseConnection(connection);
          commentToOut(INFO_LDAPMODIFY_CONNECTION_ESTABLISHED.get(hostPort));
          out();
        }
        catch (final LDAPException le)
        {
          Debug.debugException(le);
          // This should never happen.
        }
      }


      // If we should process the operations in a transaction, then start that
      // now.
      final ASN1OctetString txnID;
      if (useTransaction.isPresent())
      {
        final Control[] startTxnControls;
        if (proxyAs.isPresent())
        {
          // In a transaction, the proxied authorization control must only be
          // used in the start transaction request and not in any of the
          // subsequent operation requests.
          startTxnControls = new Control[]
          {
            new ProxiedAuthorizationV2RequestControl(proxyAs.getValue())
          };
        }
        else if (proxyV1As.isPresent())
        {
          // In a transaction, the proxied authorization control must only be
          // used in the start transaction request and not in any of the
          // subsequent operation requests.
          startTxnControls = new Control[]
          {
            new ProxiedAuthorizationV1RequestControl(proxyV1As.getValue())
          };
        }
        else
        {
          startTxnControls = StaticUtils.NO_CONTROLS;
        }

        try
        {
          final StartTransactionExtendedResult startTxnResult =
               (StartTransactionExtendedResult)
               connectionPool.processExtendedOperation(
                    new StartTransactionExtendedRequest(startTxnControls));
          if (startTxnResult.getResultCode() == ResultCode.SUCCESS)
          {
            txnID = startTxnResult.getTransactionID();

            final TransactionSpecificationRequestControl c =
                 new TransactionSpecificationRequestControl(txnID);
            addControls.add(c);
            deleteControls.add(c);
            modifyControls.add(c);
            modifyDNControls.add(c);

            final String txnIDString;
            if (StaticUtils.isPrintableString(txnID.getValue()))
            {
              txnIDString = txnID.stringValue();
            }
            else
            {
              final StringBuilder hexBuffer = new StringBuilder();
              StaticUtils.toHex(txnID.getValue(), ":", hexBuffer);
              txnIDString = hexBuffer.toString();
            }

            commentToOut(INFO_LDAPMODIFY_STARTED_TXN.get(txnIDString));
          }
          else
          {
            commentToErr(ERR_LDAPMODIFY_CANNOT_START_TXN.get(
                 startTxnResult.getResultString()));
            return startTxnResult.getResultCode();
          }
        }
        catch (final LDAPException le)
        {
          Debug.debugException(le);
          commentToErr(ERR_LDAPMODIFY_CANNOT_START_TXN.get(
               StaticUtils.getExceptionMessage(le)));
          return le.getResultCode();
        }
      }
      else
      {
        txnID = null;
      }


      // Create an LDIF reader that will be used to read the changes to process.
      if (ldifFile.isPresent())
      {
        final File[] ldifFiles = ldifFile.getValues().toArray(new File[0]);
        try
        {
          ldifReader = new LDIFReader(ldifFiles, 0, null, null,
               characterSet.getValue());
        }
        catch (final Exception e)
        {
          Debug.debugException(e);
          commentToErr(ERR_LDAPMODIFY_CANNOT_CREATE_LDIF_READER.get(
               StaticUtils.getExceptionMessage(e)));
          return ResultCode.LOCAL_ERROR;
        }
      }
      else
      {
        ldifReader = new LDIFReader(in, 0, null, null, characterSet.getValue());
      }

      if (stripTrailingSpaces.isPresent())
      {
        ldifReader.setTrailingSpaceBehavior(TrailingSpaceBehavior.STRIP);
      }


      // If appropriate, create a reject writer.
      if (rejectFile.isPresent())
      {
        try
        {
          rejectWriter = new LDIFWriter(rejectFile.getValue());

          // Set the maximum allowed wrap column.  This is better than setting a
          // wrap column of zero because it will ensure that comments don't get
          // wrapped either.
          rejectWriter.setWrapColumn(Integer.MAX_VALUE);
        }
        catch (final Exception e)
        {
          Debug.debugException(e);
          commentToErr(ERR_LDAPMODIFY_CANNOT_CREATE_REJECT_WRITER.get(
               rejectFile.getValue().getAbsolutePath(),
               StaticUtils.getExceptionMessage(e)));
          return ResultCode.LOCAL_ERROR;
        }
      }


      // If appropriate, create a rate limiter.
      final FixedRateBarrier rateLimiter;
      if (ratePerSecond.isPresent())
      {
        rateLimiter = new FixedRateBarrier(1000L, ratePerSecond.getValue());
      }
      else
      {
        rateLimiter = null;
      }


      // Iterate through the set of changes to process.
      boolean commitTransaction = true;
      ResultCode resultCode = null;
      final ArrayList<LDAPRequest> multiUpdateRequests =
           new ArrayList<LDAPRequest>(10);
      final boolean isBulkModify = modifyEntriesMatchingFilter.isPresent() ||
           modifyEntriesMatchingFiltersFromFile.isPresent() ||
           modifyEntryWithDN.isPresent() ||
           modifyEntriesWithDNsFromFile.isPresent();
readChangeRecordLoop:
      while (true)
      {
        // If there is a rate limiter, then use it to sleep if necessary.
        if ((rateLimiter != null) && (! isBulkModify))
        {
          rateLimiter.await();
        }


        // Read the next LDIF change record.  If we get an error then handle it
        // and abort if appropriate.
        final LDIFChangeRecord changeRecord;
        try
        {
          changeRecord = ldifReader.readChangeRecord(defaultAdd.isPresent());
        }
        catch (final IOException ioe)
        {
          Debug.debugException(ioe);

          final String message = ERR_LDAPMODIFY_IO_ERROR_READING_CHANGE.get(
               StaticUtils.getExceptionMessage(ioe));
          commentToErr(message);
          writeRejectedChange(rejectWriter, message, null);
          commitTransaction = false;
          resultCode = ResultCode.LOCAL_ERROR;
          break;
        }
        catch (final LDIFException le)
        {
          Debug.debugException(le);

          final StringBuilder buffer = new StringBuilder();
          if (le.mayContinueReading() && (! useTransaction.isPresent()))
          {
            buffer.append(
                 ERR_LDAPMODIFY_RECOVERABLE_LDIF_ERROR_READING_CHANGE.get(
                      le.getLineNumber(), StaticUtils.getExceptionMessage(le)));
          }
          else
          {
            buffer.append(
                 ERR_LDAPMODIFY_UNRECOVERABLE_LDIF_ERROR_READING_CHANGE.get(
                      le.getLineNumber(), StaticUtils.getExceptionMessage(le)));
          }

          if ((resultCode == null) || (resultCode == ResultCode.SUCCESS))
          {
            resultCode = ResultCode.LOCAL_ERROR;
          }

          if ((le.getDataLines() != null) && (! le.getDataLines().isEmpty()))
          {
            buffer.append(StaticUtils.EOL);
            buffer.append(StaticUtils.EOL);
            buffer.append(ERR_LDAPMODIFY_INVALID_LINES.get());
            buffer.append(StaticUtils.EOL);
            for (final String s : le.getDataLines())
            {
              buffer.append(s);
              buffer.append(StaticUtils.EOL);
            }
          }

          final String message = buffer.toString();
          commentToErr(message);
          writeRejectedChange(rejectWriter, message, null);

          if (le.mayContinueReading() && (! useTransaction.isPresent()))
          {
            continue;
          }
          else
          {
            commitTransaction = false;
            resultCode = ResultCode.LOCAL_ERROR;
            break;
          }
        }


        // If we read a null change record, then there are no more changes to
        // process.  Otherwise, treat it appropriately based on the operation
        // type.
        if (changeRecord == null)
        {
          break;
        }


        // If we should modify entries matching a specified filter, then convert
        // the change record into a set of modifications.
        if (modifyEntriesMatchingFilter.isPresent())
        {
          for (final Filter filter : modifyEntriesMatchingFilter.getValues())
          {
            final ResultCode rc = handleModifyMatchingFilter(connectionPool,
                 changeRecord,
                 modifyEntriesMatchingFilter.getIdentifierString(),
                 filter, searchControls, modifyControls, rateLimiter,
                 rejectWriter);
            if (rc != ResultCode.SUCCESS)
            {
              if ((resultCode == null) || (resultCode == ResultCode.SUCCESS) ||
                   (resultCode == ResultCode.NO_OPERATION))
              {
                resultCode = rc;
              }
            }
          }
        }

        if (modifyEntriesMatchingFiltersFromFile.isPresent())
        {
          for (final File f : modifyEntriesMatchingFiltersFromFile.getValues())
          {
            final FilterFileReader filterReader;
            try
            {
              filterReader = new FilterFileReader(f);
            }
            catch (final Exception e)
            {
              Debug.debugException(e);
              commentToErr(ERR_LDAPMODIFY_ERROR_OPENING_FILTER_FILE.get(
                   f.getAbsolutePath(), StaticUtils.getExceptionMessage(e)));
              return ResultCode.LOCAL_ERROR;
            }

            try
            {
              while (true)
              {
                final Filter filter;
                try
                {
                  filter = filterReader.readFilter();
                }
                catch (final IOException ioe)
                {
                  Debug.debugException(ioe);
                  commentToErr(ERR_LDAPMODIFY_IO_ERROR_READING_FILTER_FILE.get(
                       f.getAbsolutePath(),
                       StaticUtils.getExceptionMessage(ioe)));
                  return ResultCode.LOCAL_ERROR;
                }
                catch (final LDAPException le)
                {
                  Debug.debugException(le);
                  commentToErr(le.getMessage());
                  if (continueOnError.isPresent())
                  {
                    if ((resultCode == null) ||
                        (resultCode == ResultCode.SUCCESS) ||
                        (resultCode == ResultCode.NO_OPERATION))
                    {
                      resultCode = le.getResultCode();
                    }
                    continue;
                  }
                  else
                  {
                    return le.getResultCode();
                  }
                }

                if (filter == null)
                {
                  break;
                }

                final ResultCode rc = handleModifyMatchingFilter(connectionPool,
                     changeRecord,
                     modifyEntriesMatchingFiltersFromFile.getIdentifierString(),
                     filter, searchControls, modifyControls, rateLimiter,
                     rejectWriter);
                if (rc != ResultCode.SUCCESS)
                {
                  if ((resultCode == null) ||
                      (resultCode == ResultCode.SUCCESS) ||
                      (resultCode == ResultCode.NO_OPERATION))
                  {
                    resultCode = rc;
                  }
                }
              }
            }
            finally
            {
              try
              {
                filterReader.close();
              }
              catch (final Exception e)
              {
                Debug.debugException(e);
              }
            }
          }
        }

        if (modifyEntryWithDN.isPresent())
        {
          for (final DN dn : modifyEntryWithDN.getValues())
          {
            final ResultCode rc = handleModifyWithDN(connectionPool,
                 changeRecord, modifyEntryWithDN.getIdentifierString(), dn,
                 modifyControls, rateLimiter, rejectWriter);
            if (rc != ResultCode.SUCCESS)
            {
              if ((resultCode == null) || (resultCode == ResultCode.SUCCESS) ||
                   (resultCode == ResultCode.NO_OPERATION))
              {
                resultCode = rc;
              }
            }
          }
        }

        if (modifyEntriesWithDNsFromFile.isPresent())
        {
          for (final File f : modifyEntriesWithDNsFromFile.getValues())
          {
            final DNFileReader dnReader;
            try
            {
              dnReader = new DNFileReader(f);
            }
            catch (final Exception e)
            {
              Debug.debugException(e);
              commentToErr(ERR_LDAPMODIFY_ERROR_OPENING_DN_FILE.get(
                   f.getAbsolutePath(), StaticUtils.getExceptionMessage(e)));
              return ResultCode.LOCAL_ERROR;
            }

            try
            {
              while (true)
              {
                final DN dn;
                try
                {
                  dn = dnReader.readDN();
                }
                catch (final IOException ioe)
                {
                  Debug.debugException(ioe);
                  commentToErr(ERR_LDAPMODIFY_IO_ERROR_READING_DN_FILE.get(
                       f.getAbsolutePath(),
                       StaticUtils.getExceptionMessage(ioe)));
                  return ResultCode.LOCAL_ERROR;
                }
                catch (final LDAPException le)
                {
                  Debug.debugException(le);
                  commentToErr(le.getMessage());
                  if (continueOnError.isPresent())
                  {
                    if ((resultCode == null) ||
                        (resultCode == ResultCode.SUCCESS) ||
                        (resultCode == ResultCode.NO_OPERATION))
                    {
                      resultCode = le.getResultCode();
                    }
                    continue;
                  }
                  else
                  {
                    return le.getResultCode();
                  }
                }

                if (dn == null)
                {
                  break;
                }

                final ResultCode rc = handleModifyWithDN(connectionPool,
                     changeRecord,
                     modifyEntriesWithDNsFromFile.getIdentifierString(), dn,
                     modifyControls, rateLimiter, rejectWriter);
                if (rc != ResultCode.SUCCESS)
                {
                  if ((resultCode == null) ||
                      (resultCode == ResultCode.SUCCESS) ||
                      (resultCode == ResultCode.NO_OPERATION))
                  {
                    resultCode = rc;
                  }
                }
              }
            }
            finally
            {
              try
              {
                dnReader.close();
              }
              catch (final Exception e)
              {
                Debug.debugException(e);
              }
            }
          }
        }

        if (isBulkModify)
        {
          continue;
        }

        try
        {
          final ResultCode rc;
          if (changeRecord instanceof LDIFAddChangeRecord)
          {
            rc = doAdd((LDIFAddChangeRecord) changeRecord, addControls,
                 connectionPool, multiUpdateRequests, rejectWriter);
          }
          else if (changeRecord instanceof LDIFDeleteChangeRecord)
          {
            rc = doDelete((LDIFDeleteChangeRecord) changeRecord, deleteControls,
                 connectionPool, multiUpdateRequests, rejectWriter);
          }
          else if (changeRecord instanceof LDIFModifyChangeRecord)
          {
            rc = doModify((LDIFModifyChangeRecord) changeRecord, modifyControls,
                 connectionPool, multiUpdateRequests, rejectWriter);
          }
          else if (changeRecord instanceof LDIFModifyDNChangeRecord)
          {
            rc = doModifyDN((LDIFModifyDNChangeRecord) changeRecord,
                 modifyDNControls, connectionPool, multiUpdateRequests,
                 rejectWriter);
          }
          else
          {
            // This should never happen.
            commentToErr(ERR_LDAPMODIFY_UNSUPPORTED_CHANGE_RECORD_HEADER.get());
            for (final String line : changeRecord.toLDIF())
            {
              err("#      " + line);
            }
            throw new LDAPException(ResultCode.PARAM_ERROR,
                 ERR_LDAPMODIFY_UNSUPPORTED_CHANGE_RECORD_HEADER.get() +
                      changeRecord.toString());
          }

          if ((resultCode == null) && (rc != ResultCode.SUCCESS))
          {
            resultCode = rc;
          }
        }
        catch (final LDAPException le)
        {
          Debug.debugException(le);

          commitTransaction = false;
          if (continueOnError.isPresent())
          {
            if ((resultCode == null) || (resultCode == ResultCode.SUCCESS) ||
                 (resultCode == ResultCode.NO_OPERATION))
            {
              resultCode = le.getResultCode();
            }
          }
          else
          {
            resultCode = le.getResultCode();
            break;
          }
        }
      }


      // If the operations are part of a transaction, then commit or abort that
      // transaction now.  Otherwise, if they should be part of a multi-update
      // operation, then process that now.
      if (useTransaction.isPresent())
      {
        LDAPResult endTxnResult;
        final EndTransactionExtendedRequest endTxnRequest =
             new EndTransactionExtendedRequest(txnID, commitTransaction);
        try
        {
          endTxnResult = connectionPool.processExtendedOperation(endTxnRequest);
        }
        catch (final LDAPException le)
        {
          endTxnResult = le.toLDAPResult();
        }

        displayResult(endTxnResult, false);
        if (((resultCode == null) || (resultCode == ResultCode.SUCCESS)) &&
            (endTxnResult.getResultCode() != ResultCode.SUCCESS))
        {
          resultCode = endTxnResult.getResultCode();
        }
      }
      else if (multiUpdateErrorBehavior.isPresent())
      {
        final MultiUpdateErrorBehavior errorBehavior;
        if (multiUpdateErrorBehavior.getValue().equalsIgnoreCase("atomic"))
        {
          errorBehavior = MultiUpdateErrorBehavior.ATOMIC;
        }
        else if (multiUpdateErrorBehavior.getValue().equalsIgnoreCase(
                      "abort-on-error"))
        {
          errorBehavior = MultiUpdateErrorBehavior.ABORT_ON_ERROR;
        }
        else
        {
          errorBehavior = MultiUpdateErrorBehavior.CONTINUE_ON_ERROR;
        }

        final Control[] multiUpdateControls;
        if (proxyAs.isPresent())
        {
          multiUpdateControls = new Control[]
          {
            new ProxiedAuthorizationV2RequestControl(proxyAs.getValue())
          };
        }
        else if (proxyV1As.isPresent())
        {
          multiUpdateControls = new Control[]
          {
            new ProxiedAuthorizationV1RequestControl(proxyV1As.getValue())
          };
        }
        else
        {
          multiUpdateControls = StaticUtils.NO_CONTROLS;
        }

        ExtendedResult multiUpdateResult;
        try
        {
          commentToOut(INFO_LDAPMODIFY_SENDING_MULTI_UPDATE_REQUEST.get());
          final MultiUpdateExtendedRequest multiUpdateRequest =
               new MultiUpdateExtendedRequest(errorBehavior,
                    multiUpdateRequests, multiUpdateControls);
          multiUpdateResult =
               connectionPool.processExtendedOperation(multiUpdateRequest);
        }
        catch (final LDAPException le)
        {
          multiUpdateResult = new ExtendedResult(le);
        }

        displayResult(multiUpdateResult, false);
        resultCode = multiUpdateResult.getResultCode();
      }


      if (resultCode == null)
      {
        return ResultCode.SUCCESS;
      }
      else
      {
        return resultCode;
      }
    }
    finally
    {
      if (rejectWriter != null)
      {
        try
        {
          rejectWriter.close();
        }
        catch (final Exception e)
        {
          Debug.debugException(e);
        }
      }

      if (ldifReader != null)
      {
        try
        {
          ldifReader.close();
        }
        catch (final Exception e)
        {
          Debug.debugException(e);
        }
      }

      if (connectionPool != null)
      {
        try
        {
          connectionPool.close();
        }
        catch (final Exception e)
        {
          Debug.debugException(e);
        }
      }
    }
  }



  /**
   * Handles the processing for a change record when the tool should modify
   * entries matching a given filter.
   *
   * @param  connectionPool       The connection pool to use to communicate with
   *                              the directory server.
   * @param  changeRecord         The LDIF change record to be processed.
   * @param  argIdentifierString  The identifier string for the argument used to
   *                              specify the filter to use to identify the
   *                              entries to modify.
   * @param  filter               The filter to use to identify the entries to
   *                              modify.
   * @param  searchControls       The set of controls to include in the search
   *                              request.
   * @param  modifyControls       The set of controls to include in the modify
   *                              requests.
   * @param  rateLimiter          The fixed-rate barrier to use for rate
   *                              limiting.  It may be {@code null} if no rate
   *                              limiting is required.
   * @param  rejectWriter         The reject writer to use to record information
   *                              about any failed operations.
   *
   * @return  A result code obtained from processing.
   */
  private ResultCode handleModifyMatchingFilter(
                          final LDAPConnectionPool connectionPool,
                          final LDIFChangeRecord changeRecord,
                          final String argIdentifierString, final Filter filter,
                          final List<Control> searchControls,
                          final List<Control> modifyControls,
                          final FixedRateBarrier rateLimiter,
                          final LDIFWriter rejectWriter)
  {
    // If the provided change record isn't a modify change record, then that's
    // an error.  Reject it.
    if (! (changeRecord instanceof LDIFModifyChangeRecord))
    {
      writeRejectedChange(rejectWriter,
           ERR_LDAPMODIFY_NON_MODIFY_WITH_BULK.get(argIdentifierString),
           changeRecord);
      return ResultCode.PARAM_ERROR;
    }

    final LDIFModifyChangeRecord modifyChangeRecord =
         (LDIFModifyChangeRecord) changeRecord;
    final HashSet<DN> processedDNs = new HashSet<DN>(100);


    // If we need to use the simple paged results control, then we may have to
    // issue multiple searches.
    ASN1OctetString pagedResultsCookie = null;
    long entriesProcessed = 0L;
    ResultCode resultCode = ResultCode.SUCCESS;
    while (true)
    {
      // Construct the search request to send.
      final LDAPModifySearchListener listener =
           new LDAPModifySearchListener(this, modifyChangeRecord, filter,
                modifyControls, connectionPool, rateLimiter, rejectWriter,
                processedDNs);

      final SearchRequest searchRequest =
           new SearchRequest(listener, modifyChangeRecord.getDN(),
                SearchScope.SUB, filter, SearchRequest.NO_ATTRIBUTES);
      searchRequest.setControls(searchControls);
      if (searchPageSize.isPresent())
      {
        searchRequest.addControl(new SimplePagedResultsControl(
             searchPageSize.getValue(), pagedResultsCookie));
      }


      // The connection pool's automatic retry feature can't work for searches
      // that return one or more entries before encountering a failure.  To get
      // around that, we'll check a connection out of the pool and use it to
      // process the search.  If an error occurs that indicates the connection
      // is no longer valid, we can replace it with a newly-established
      // connection and try again.  The search result listener will ensure that
      // no entry gets updated twice.
      LDAPConnection connection;
      try
      {
        connection = connectionPool.getConnection();
      }
      catch (final LDAPException le)
      {
        Debug.debugException(le);

        writeRejectedChange(rejectWriter,
             ERR_LDAPMODIFY_CANNOT_GET_SEARCH_CONNECTION.get(
                  modifyChangeRecord.getDN(), String.valueOf(filter),
                  StaticUtils.getExceptionMessage(le)),
             modifyChangeRecord, le.toLDAPResult());
        return le.getResultCode();
      }

      SearchResult searchResult;
      boolean connectionValid = false;
      try
      {
        try
        {
          searchResult = connection.search(searchRequest);
        }
        catch (final LDAPSearchException lse)
        {
          searchResult = lse.getSearchResult();
        }

        if (searchResult.getResultCode() == ResultCode.SUCCESS)
        {
          connectionValid = true;
        }
        else if (searchResult.getResultCode().isConnectionUsable())
        {
          connectionValid = true;
          writeRejectedChange(rejectWriter,
               ERR_LDAPMODIFY_SEARCH_FAILED.get(modifyChangeRecord.getDN(),
                    String.valueOf(filter)),
               modifyChangeRecord, searchResult);
          return searchResult.getResultCode();
        }
        else if (retryFailedOperations.isPresent())
        {
          try
          {
            connection = connectionPool.replaceDefunctConnection(connection);
          }
          catch (final LDAPException le)
          {
            Debug.debugException(le);
            writeRejectedChange(rejectWriter,
                 ERR_LDAPMODIFY_SEARCH_FAILED_CANNOT_RECONNECT.get(
                      modifyChangeRecord.getDN(), String.valueOf(filter)),
                 modifyChangeRecord, searchResult);
            return searchResult.getResultCode();
          }

          try
          {
            searchResult = connection.search(searchRequest);
          }
          catch (final LDAPSearchException lse)
          {
            Debug.debugException(lse);
            searchResult = lse.getSearchResult();
          }

          if (searchResult.getResultCode() == ResultCode.SUCCESS)
          {
            connectionValid = true;
          }
          else
          {
            connectionValid = searchResult.getResultCode().isConnectionUsable();
            writeRejectedChange(rejectWriter,
                 ERR_LDAPMODIFY_SEARCH_FAILED.get(modifyChangeRecord.getDN(),
                      String.valueOf(filter)),
                 modifyChangeRecord, searchResult);
            return searchResult.getResultCode();
          }
        }
        else
        {
          writeRejectedChange(rejectWriter,
               ERR_LDAPMODIFY_SEARCH_FAILED.get(modifyChangeRecord.getDN(),
                    String.valueOf(filter)),
               modifyChangeRecord, searchResult);
          return searchResult.getResultCode();
        }
      }
      finally
      {
        if (connectionValid)
        {
          connectionPool.releaseConnection(connection);
        }
        else
        {
          connectionPool.releaseDefunctConnection(connection);
        }
      }


      // If we've gotten here, then the search was successful.  Check to see if
      // any of the modifications failed, and if so then update the result code
      // accordingly.
      if ((resultCode == ResultCode.SUCCESS) &&
          (listener.getResultCode() != ResultCode.SUCCESS))
      {
        resultCode = listener.getResultCode();
      }


      // If the search used the simple paged results control then we may need to
      // repeat the search to get the next page.
      entriesProcessed += searchResult.getEntryCount();
      if (searchPageSize.isPresent())
      {
        final SimplePagedResultsControl responseControl;
        try
        {
          responseControl = SimplePagedResultsControl.get(searchResult);
        }
        catch (final LDAPException le)
        {
          Debug.debugException(le);
          writeRejectedChange(rejectWriter,
               ERR_LDAPMODIFY_CANNOT_DECODE_PAGED_RESULTS_CONTROL.get(
                    modifyChangeRecord.getDN(), String.valueOf(filter)),
               modifyChangeRecord, le.toLDAPResult());
          return le.getResultCode();
        }

        if (responseControl == null)
        {
          writeRejectedChange(rejectWriter,
               ERR_LDAPMODIFY_MISSING_PAGED_RESULTS_RESPONSE.get(
                    modifyChangeRecord.getDN(), String.valueOf(filter)),
               modifyChangeRecord);
          return ResultCode.CONTROL_NOT_FOUND;
        }
        else
        {
          pagedResultsCookie = responseControl.getCookie();
          if (responseControl.moreResultsToReturn())
          {
            if (verbose.isPresent())
            {
              commentToOut(INFO_LDAPMODIFY_SEARCH_COMPLETED_MORE_PAGES.get(
                   modifyChangeRecord.getDN(), String.valueOf(filter),
                   entriesProcessed));
              for (final String resultLine :
                   ResultUtils.formatResult(searchResult, true, 0, WRAP_COLUMN))
              {
                out(resultLine);
              }
              out();
            }
          }
          else
          {
            commentToOut(INFO_LDAPMODIFY_SEARCH_COMPLETED.get(
                 entriesProcessed, modifyChangeRecord.getDN(),
                 String.valueOf(filter)));
            if (verbose.isPresent())
            {
              for (final String resultLine :
                   ResultUtils.formatResult(searchResult, true, 0, WRAP_COLUMN))
              {
                out(resultLine);
              }
            }

            out();
            return resultCode;
          }
        }
      }
      else
      {
        commentToOut(INFO_LDAPMODIFY_SEARCH_COMPLETED.get(
             entriesProcessed, modifyChangeRecord.getDN(),
             String.valueOf(filter)));
        if (verbose.isPresent())
        {
          for (final String resultLine :
               ResultUtils.formatResult(searchResult, true, 0, WRAP_COLUMN))
          {
            out(resultLine);
          }
        }

        out();
        return resultCode;
      }
    }
  }



  /**
   * Handles the processing for a change record when the tool should modify an
   * entry with a given DN instead of the DN contained in the change record.
   *
   * @param  connectionPool       The connection pool to use to communicate with
   *                              the directory server.
   * @param  changeRecord         The LDIF change record to be processed.
   * @param  argIdentifierString  The identifier string for the argument used to
   *                              specify the DN of the entry to modify.
   * @param  dn                   The DN of the entry to modify.
   * @param  modifyControls       The set of controls to include in the modify
   *                              requests.
   * @param  rateLimiter          The fixed-rate barrier to use for rate
   *                              limiting.  It may be {@code null} if no rate
   *                              limiting is required.
   * @param  rejectWriter         The reject writer to use to record information
   *                              about any failed operations.
   *
   * @return  A result code obtained from processing.
   */
  private ResultCode handleModifyWithDN(
                          final LDAPConnectionPool connectionPool,
                          final LDIFChangeRecord changeRecord,
                          final String argIdentifierString, final DN dn,
                          final List<Control> modifyControls,
                          final FixedRateBarrier rateLimiter,
                          final LDIFWriter rejectWriter)
  {
    // If the provided change record isn't a modify change record, then that's
    // an error.  Reject it.
    if (! (changeRecord instanceof LDIFModifyChangeRecord))
    {
      writeRejectedChange(rejectWriter,
           ERR_LDAPMODIFY_NON_MODIFY_WITH_BULK.get(argIdentifierString),
           changeRecord);
      return ResultCode.PARAM_ERROR;
    }


    // Create a new modify change record with the provided DN instead of the
    // original DN.
    final LDIFModifyChangeRecord originalChangeRecord =
         (LDIFModifyChangeRecord) changeRecord;
    final LDIFModifyChangeRecord updatedChangeRecord =
         new LDIFModifyChangeRecord(dn.toString(),
              originalChangeRecord.getModifications(),
              originalChangeRecord.getControls());

    if (rateLimiter != null)
    {
      rateLimiter.await();
    }

    try
    {
      return doModify(updatedChangeRecord, modifyControls, connectionPool, null,
           rejectWriter);
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      return le.getResultCode();
    }
  }



  /**
   * Populates lists of request controls that should be included in requests
   * of various types.
   *
   * @param  addControls       The list of controls to include in add requests.
   * @param  deleteControls    The list of controls to include in delete
   *                           requests.
   * @param  modifyControls    The list of controls to include in modify
   *                           requests.
   * @param  modifyDNControls  The list of controls to include in modify DN
   *                           requests.
   * @param  searchControls    The list of controls to include in search
   *                           requests.
   */
  private void createRequestControls(final List<Control> addControls,
                                     final List<Control> deleteControls,
                                     final List<Control> modifyControls,
                                     final List<Control> modifyDNControls,
                                     final List<Control> searchControls)
  {
    if (addControl.isPresent())
    {
      addControls.addAll(addControl.getValues());
    }

    if (deleteControl.isPresent())
    {
      deleteControls.addAll(deleteControl.getValues());
    }

    if (modifyControl.isPresent())
    {
      modifyControls.addAll(modifyControl.getValues());
    }

    if (modifyDNControl.isPresent())
    {
      modifyDNControls.addAll(modifyDNControl.getValues());
    }

    if (operationControl.isPresent())
    {
      addControls.addAll(operationControl.getValues());
      deleteControls.addAll(operationControl.getValues());
      modifyControls.addAll(operationControl.getValues());
      modifyDNControls.addAll(operationControl.getValues());
    }

    if (noOperation.isPresent())
    {
      final NoOpRequestControl c = new NoOpRequestControl();
      addControls.add(c);
      deleteControls.add(c);
      modifyControls.add(c);
      modifyDNControls.add(c);
    }

    if (ignoreNoUserModification.isPresent())
    {
      addControls.add(new IgnoreNoUserModificationRequestControl());
    }

    if (nameWithEntryUUID.isPresent())
    {
      addControls.add(new NameWithEntryUUIDRequestControl(true));
    }

    if (permissiveModify.isPresent())
    {
      modifyControls.add(new PermissiveModifyRequestControl(false));
    }

    if (suppressReferentialIntegrityUpdates.isPresent())
    {
      final SuppressReferentialIntegrityUpdatesRequestControl c =
           new SuppressReferentialIntegrityUpdatesRequestControl(true);
      deleteControls.add(c);
      modifyDNControls.add(c);
    }

    if (suppressOperationalAttributeUpdates.isPresent())
    {
      final EnumSet<SuppressType> suppressTypes =
           EnumSet.noneOf(SuppressType.class);
      for (final String s : suppressOperationalAttributeUpdates.getValues())
      {
        if (s.equalsIgnoreCase("last-access-time"))
        {
          suppressTypes.add(SuppressType.LAST_ACCESS_TIME);
        }
        else if (s.equalsIgnoreCase("last-login-time"))
        {
          suppressTypes.add(SuppressType.LAST_LOGIN_TIME);
        }
        else if (s.equalsIgnoreCase("last-login-ip"))
        {
          suppressTypes.add(SuppressType.LAST_LOGIN_IP);
        }
        else if (s.equalsIgnoreCase("lastmod"))
        {
          suppressTypes.add(SuppressType.LASTMOD);
        }
      }

      final SuppressOperationalAttributeUpdateRequestControl c =
           new SuppressOperationalAttributeUpdateRequestControl(suppressTypes);
      addControls.add(c);
      deleteControls.add(c);
      modifyControls.add(c);
      modifyDNControls.add(c);
    }

    if (usePasswordPolicyControl.isPresent())
    {
      final PasswordPolicyRequestControl c = new PasswordPolicyRequestControl();
      addControls.add(c);
      modifyControls.add(c);
    }

    if (assuredReplication.isPresent())
    {
      AssuredReplicationLocalLevel localLevel = null;
      if (assuredReplicationLocalLevel.isPresent())
      {
        final String level = assuredReplicationLocalLevel.getValue();
        if (level.equalsIgnoreCase("none"))
        {
          localLevel = AssuredReplicationLocalLevel.NONE;
        }
        else if (level.equalsIgnoreCase("received-any-server"))
        {
          localLevel = AssuredReplicationLocalLevel.RECEIVED_ANY_SERVER;
        }
        else if (level.equalsIgnoreCase("processed-all-servers"))
        {
          localLevel = AssuredReplicationLocalLevel.PROCESSED_ALL_SERVERS;
        }
      }

      AssuredReplicationRemoteLevel remoteLevel = null;
      if (assuredReplicationRemoteLevel.isPresent())
      {
        final String level = assuredReplicationRemoteLevel.getValue();
        if (level.equalsIgnoreCase("none"))
        {
          remoteLevel = AssuredReplicationRemoteLevel.NONE;
        }
        else if (level.equalsIgnoreCase("received-any-remote-location"))
        {
          remoteLevel =
               AssuredReplicationRemoteLevel.RECEIVED_ANY_REMOTE_LOCATION;
        }
        else if (level.equalsIgnoreCase("received-all-remote-locations"))
        {
          remoteLevel =
               AssuredReplicationRemoteLevel.RECEIVED_ALL_REMOTE_LOCATIONS;
        }
        else if (level.equalsIgnoreCase("processed-all-remote-servers"))
        {
          remoteLevel =
               AssuredReplicationRemoteLevel.PROCESSED_ALL_REMOTE_SERVERS;
        }
      }

      Long timeoutMillis = null;
      if (assuredReplicationTimeout.isPresent())
      {
        timeoutMillis =
             assuredReplicationTimeout.getValue(TimeUnit.MILLISECONDS);
      }

      final AssuredReplicationRequestControl c =
           new AssuredReplicationRequestControl(true, localLevel, localLevel,
                remoteLevel, remoteLevel, timeoutMillis, false);
      addControls.add(c);
      deleteControls.add(c);
      modifyControls.add(c);
      modifyDNControls.add(c);
    }

    if (hardDelete.isPresent())
    {
      deleteControls.add(new HardDeleteRequestControl(true));
    }

    if (replicationRepair.isPresent())
    {
      final ReplicationRepairRequestControl c =
           new ReplicationRepairRequestControl();
      addControls.add(c);
      deleteControls.add(c);
      modifyControls.add(c);
      modifyDNControls.add(c);
    }

    if (softDelete.isPresent())
    {
      deleteControls.add(new SoftDeleteRequestControl(true, true));
    }

    if (subtreeDelete.isPresent())
    {
      deleteControls.add(new SubtreeDeleteRequestControl());
    }

    if (assertionFilter.isPresent())
    {
      final AssertionRequestControl c = new AssertionRequestControl(
           assertionFilter.getValue(), true);
      addControls.add(c);
      deleteControls.add(c);
      modifyControls.add(c);
      modifyDNControls.add(c);
    }

    if (operationPurpose.isPresent())
    {
      final OperationPurposeRequestControl c =
           new OperationPurposeRequestControl(false, "ldapmodify",
                Version.NUMERIC_VERSION_STRING,
                LDAPModify.class.getName() + ".createRequestControls",
                operationPurpose.getValue());
      addControls.add(c);
      deleteControls.add(c);
      modifyControls.add(c);
      modifyDNControls.add(c);
    }

    if (manageDsaIT.isPresent())
    {
      final ManageDsaITRequestControl c = new ManageDsaITRequestControl(true);
      addControls.add(c);
      deleteControls.add(c);
      modifyControls.add(c);
      modifyDNControls.add(c);
    }

    if (preReadAttribute.isPresent())
    {
      final ArrayList<String> attrList = new ArrayList<String>(10);
      for (final String value : preReadAttribute.getValues())
      {
        final StringTokenizer tokenizer = new StringTokenizer(value, ", ");
        while (tokenizer.hasMoreTokens())
        {
          attrList.add(tokenizer.nextToken());
        }
      }

      final String[] attrArray = attrList.toArray(StaticUtils.NO_STRINGS);
      final PreReadRequestControl c = new PreReadRequestControl(attrArray);
      deleteControls.add(c);
      modifyControls.add(c);
      modifyDNControls.add(c);
    }

    if (postReadAttribute.isPresent())
    {
      final ArrayList<String> attrList = new ArrayList<String>(10);
      for (final String value : postReadAttribute.getValues())
      {
        final StringTokenizer tokenizer = new StringTokenizer(value, ", ");
        while (tokenizer.hasMoreTokens())
        {
          attrList.add(tokenizer.nextToken());
        }
      }

      final String[] attrArray = attrList.toArray(StaticUtils.NO_STRINGS);
      final PostReadRequestControl c = new PostReadRequestControl(attrArray);
      addControls.add(c);
      modifyControls.add(c);
      modifyDNControls.add(c);
    }

    if (proxyAs.isPresent() && (! useTransaction.isPresent()) &&
        (! multiUpdateErrorBehavior.isPresent()))
    {
      final ProxiedAuthorizationV2RequestControl c =
           new ProxiedAuthorizationV2RequestControl(proxyAs.getValue());
      addControls.add(c);
      deleteControls.add(c);
      modifyControls.add(c);
      modifyDNControls.add(c);
      searchControls.add(c);
    }

    if (proxyV1As.isPresent() && (! useTransaction.isPresent()) &&
        (! multiUpdateErrorBehavior.isPresent()))
    {
      final ProxiedAuthorizationV1RequestControl c =
           new ProxiedAuthorizationV1RequestControl(proxyV1As.getValue());
      addControls.add(c);
      deleteControls.add(c);
      modifyControls.add(c);
      modifyDNControls.add(c);
      searchControls.add(c);
    }
  }



  /**
   * Performs the appropriate processing for an LDIF add change record.
   *
   * @param  changeRecord         The LDIF add change record to process.
   * @param  controls             The set of controls to include in the request.
   * @param  pool                 The connection pool to use to communicate with
   *                              the directory server.
   * @param  multiUpdateRequests  The list to which the request should be added
   *                              if it is to be processed as part of a
   *                              multi-update operation.  It may be
   *                              {@code null} if the operation should not be
   *                              processed via the multi-update operation.
   * @param  rejectWriter         The LDIF writer to use for recording
   *                              information about rejected changes.  It may be
   *                              {@code null} if no reject writer is
   *                              configured.
   *
   * @return  The result code obtained from processing.
   *
   * @throws  LDAPException  If the operation did not complete successfully
   *                         and processing should not continue.
   */
  private ResultCode doAdd(final LDIFAddChangeRecord changeRecord,
                           final List<Control> controls,
                           final LDAPConnectionPool pool,
                           final List<LDAPRequest> multiUpdateRequests,
                           final LDIFWriter rejectWriter)
          throws LDAPException
  {
    // Create the add request to process.
    final AddRequest addRequest = changeRecord.toAddRequest(true);
    for (final Control c : controls)
    {
      addRequest.addControl(c);
    }


    // If we should provide support for undelete operations and the entry
    // includes the ds-undelete-from-dn attribute, then add the undelete request
    // control.
    if (allowUndelete.isPresent() &&
        addRequest.hasAttribute(ATTR_UNDELETE_FROM_DN))
    {
      addRequest.addControl(new UndeleteRequestControl());
    }


    // If the entry to add includes a password, then add a password validation
    // details request control if appropriate.
    if (passwordValidationDetails.isPresent())
    {
      final Entry entryToAdd = addRequest.toEntry();
      if ((! entryToAdd.getAttributesWithOptions(ATTR_USER_PASSWORD,
                  null).isEmpty()) ||
          (! entryToAdd.getAttributesWithOptions(ATTR_AUTH_PASSWORD,
                  null).isEmpty()))
      {
        addRequest.addControl(new PasswordValidationDetailsRequestControl());
      }
    }


    // If the operation should be processed in a multi-update operation, then
    // just add the request to the list and return without doing anything else.
    if (multiUpdateErrorBehavior.isPresent())
    {
      multiUpdateRequests.add(addRequest);
      commentToOut(INFO_LDAPMODIFY_ADD_ADDED_TO_MULTI_UPDATE.get(
           addRequest.getDN()));
      return ResultCode.SUCCESS;
    }


    // If the --dryRun argument was provided, then we'll stop here.
    if (dryRun.isPresent())
    {
      commentToOut(INFO_LDAPMODIFY_DRY_RUN_ADD.get(addRequest.getDN()));
      return ResultCode.SUCCESS;
    }


    // Process the add operation and get the result.
    commentToOut(INFO_LDAPMODIFY_ADDING_ENTRY.get(addRequest.getDN()));
    if (verbose.isPresent())
    {
      for (final String ldifLine :
           addRequest.toLDIFChangeRecord().toLDIF(WRAP_COLUMN))
      {
        out(ldifLine);
      }
      out();
    }

    LDAPResult addResult;
    try
    {
      addResult = pool.add(addRequest);
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      addResult = le.toLDAPResult();
    }


    // Display information about the result.
    displayResult(addResult, useTransaction.isPresent());


    // See if the add operation succeeded or failed.  If it failed, and we
    // should end all processing, then throw an exception.
    switch (addResult.getResultCode().intValue())
    {
      case ResultCode.SUCCESS_INT_VALUE:
      case ResultCode.NO_OPERATION_INT_VALUE:
        break;

      case ResultCode.ASSERTION_FAILED_INT_VALUE:
        writeRejectedChange(rejectWriter,
             INFO_LDAPMODIFY_ASSERTION_FAILED.get(addRequest.getDN(),
                  String.valueOf(assertionFilter.getValue())),
             addRequest.toLDIFChangeRecord(), addResult);
        throw new LDAPException(addResult);

      default:
        writeRejectedChange(rejectWriter, null, addRequest.toLDIFChangeRecord(),
             addResult);
        if (useTransaction.isPresent() || (! continueOnError.isPresent()))
        {
          throw new LDAPException(addResult);
        }
        break;
    }

    return addResult.getResultCode();
  }



  /**
   * Performs the appropriate processing for an LDIF delete change record.
   *
   * @param  changeRecord         The LDIF delete change record to process.
   * @param  controls             The set of controls to include in the request.
   * @param  pool                 The connection pool to use to communicate with
   *                              the directory server.
   * @param  multiUpdateRequests  The list to which the request should be added
   *                              if it is to be processed as part of a
   *                              multi-update operation.  It may be
   *                              {@code null} if the operation should not be
   *                              processed via the multi-update operation.
   * @param  rejectWriter         The LDIF writer to use for recording
   *                              information about rejected changes.  It may be
   *                              {@code null} if no reject writer is
   *                              configured.
   *
   * @return  The result code obtained from processing.
   *
   * @throws  LDAPException  If the operation did not complete successfully
   *                         and processing should not continue.
   */
  private ResultCode doDelete(final LDIFDeleteChangeRecord changeRecord,
                              final List<Control> controls,
                              final LDAPConnectionPool pool,
                              final List<LDAPRequest> multiUpdateRequests,
                              final LDIFWriter rejectWriter)
          throws LDAPException
  {
    // Create the delete request to process.
    final DeleteRequest deleteRequest = changeRecord.toDeleteRequest(true);
    for (final Control c : controls)
    {
      deleteRequest.addControl(c);
    }


    // If the operation should be processed in a multi-update operation, then
    // just add the request to the list and return without doing anything else.
    if (multiUpdateErrorBehavior.isPresent())
    {
      multiUpdateRequests.add(deleteRequest);
      commentToOut(INFO_LDAPMODIFY_DELETE_ADDED_TO_MULTI_UPDATE.get(
           deleteRequest.getDN()));
      return ResultCode.SUCCESS;
    }


    // If the --dryRun argument was provided, then we'll stop here.
    if (dryRun.isPresent())
    {
      commentToOut(INFO_LDAPMODIFY_DRY_RUN_DELETE.get(deleteRequest.getDN()));
      return ResultCode.SUCCESS;
    }


    // Process the delete operation and get the result.
    commentToOut(INFO_LDAPMODIFY_DELETING_ENTRY.get(deleteRequest.getDN()));
    if (verbose.isPresent())
    {
      for (final String ldifLine :
           deleteRequest.toLDIFChangeRecord().toLDIF(WRAP_COLUMN))
      {
        out(ldifLine);
      }
      out();
    }


    LDAPResult deleteResult;
    try
    {
      deleteResult = pool.delete(deleteRequest);
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      deleteResult = le.toLDAPResult();
    }


    // Display information about the result.
    displayResult(deleteResult, useTransaction.isPresent());


    // See if the delete operation succeeded or failed.  If it failed, and we
    // should end all processing, then throw an exception.
    switch (deleteResult.getResultCode().intValue())
    {
      case ResultCode.SUCCESS_INT_VALUE:
      case ResultCode.NO_OPERATION_INT_VALUE:
        break;

      case ResultCode.ASSERTION_FAILED_INT_VALUE:
        writeRejectedChange(rejectWriter,
             INFO_LDAPMODIFY_ASSERTION_FAILED.get(deleteRequest.getDN(),
                  String.valueOf(assertionFilter.getValue())),
             deleteRequest.toLDIFChangeRecord(), deleteResult);
        throw new LDAPException(deleteResult);

      default:
        writeRejectedChange(rejectWriter, null,
             deleteRequest.toLDIFChangeRecord(), deleteResult);
        if (useTransaction.isPresent() || (! continueOnError.isPresent()))
        {
          throw new LDAPException(deleteResult);
        }
        break;
    }

    return deleteResult.getResultCode();
  }



  /**
   * Performs the appropriate processing for an LDIF modify change record.
   *
   * @param  changeRecord         The LDIF modify change record to process.
   * @param  controls             The set of controls to include in the request.
   * @param  pool                 The connection pool to use to communicate with
   *                              the directory server.
   * @param  multiUpdateRequests  The list to which the request should be added
   *                              if it is to be processed as part of a
   *                              multi-update operation.  It may be
   *                              {@code null} if the operation should not be
   *                              processed via the multi-update operation.
   * @param  rejectWriter         The LDIF writer to use for recording
   *                              information about rejected changes.  It may be
   *                              {@code null} if no reject writer is
   *                              configured.
   *
   * @return  The result code obtained from processing.
   *
   * @throws  LDAPException  If the operation did not complete successfully
   *                         and processing should not continue.
   */
  ResultCode doModify(final LDIFModifyChangeRecord changeRecord,
                      final List<Control> controls,
                      final LDAPConnectionPool pool,
                      final List<LDAPRequest> multiUpdateRequests,
                      final LDIFWriter rejectWriter)
             throws LDAPException
  {
    // Create the modify request to process.
    final ModifyRequest modifyRequest = changeRecord.toModifyRequest(true);
    for (final Control c : controls)
    {
      modifyRequest.addControl(c);
    }


    // If the modify request includes a password change, then add any controls
    // that are specific to that.
    if (retireCurrentPassword.isPresent() || purgeCurrentPassword.isPresent() ||
        passwordValidationDetails.isPresent())
    {
      for (final Modification m : modifyRequest.getModifications())
      {
        final String baseName = m.getAttribute().getBaseName();
        if (baseName.equalsIgnoreCase(ATTR_USER_PASSWORD) ||
            baseName.equalsIgnoreCase(ATTR_AUTH_PASSWORD))
        {
          if (retireCurrentPassword.isPresent())
          {
            modifyRequest.addControl(new RetirePasswordRequestControl(false));
          }
          else if (purgeCurrentPassword.isPresent())
          {
            modifyRequest.addControl(new PurgePasswordRequestControl(false));
          }

          if (passwordValidationDetails.isPresent())
          {
            modifyRequest.addControl(
                 new PasswordValidationDetailsRequestControl());
          }

          break;
        }
      }
    }


    // If the operation should be processed in a multi-update operation, then
    // just add the request to the list and return without doing anything else.
    if (multiUpdateErrorBehavior.isPresent())
    {
      multiUpdateRequests.add(modifyRequest);
      commentToOut(INFO_LDAPMODIFY_MODIFY_ADDED_TO_MULTI_UPDATE.get(
           modifyRequest.getDN()));
      return ResultCode.SUCCESS;
    }


    // If the --dryRun argument was provided, then we'll stop here.
    if (dryRun.isPresent())
    {
      commentToOut(INFO_LDAPMODIFY_DRY_RUN_MODIFY.get(modifyRequest.getDN()));
      return ResultCode.SUCCESS;
    }


    // Process the modify operation and get the result.
    commentToOut(INFO_LDAPMODIFY_MODIFYING_ENTRY.get(modifyRequest.getDN()));
    if (verbose.isPresent())
    {
      for (final String ldifLine :
           modifyRequest.toLDIFChangeRecord().toLDIF(WRAP_COLUMN))
      {
        out(ldifLine);
      }
      out();
    }


    LDAPResult modifyResult;
    try
    {
      modifyResult = pool.modify(modifyRequest);
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      modifyResult = le.toLDAPResult();
    }


    // Display information about the result.
    displayResult(modifyResult, useTransaction.isPresent());


    // See if the modify operation succeeded or failed.  If it failed, and we
    // should end all processing, then throw an exception.
    switch (modifyResult.getResultCode().intValue())
    {
      case ResultCode.SUCCESS_INT_VALUE:
      case ResultCode.NO_OPERATION_INT_VALUE:
        break;

      case ResultCode.ASSERTION_FAILED_INT_VALUE:
        writeRejectedChange(rejectWriter,
             INFO_LDAPMODIFY_ASSERTION_FAILED.get(modifyRequest.getDN(),
                  String.valueOf(assertionFilter.getValue())),
             modifyRequest.toLDIFChangeRecord(), modifyResult);
        throw new LDAPException(modifyResult);

      default:
        writeRejectedChange(rejectWriter, null,
             modifyRequest.toLDIFChangeRecord(), modifyResult);
        if (useTransaction.isPresent() || (! continueOnError.isPresent()))
        {
          throw new LDAPException(modifyResult);
        }
        break;
    }

    return modifyResult.getResultCode();
  }



  /**
   * Performs the appropriate processing for an LDIF modify DN change record.
   *
   * @param  changeRecord         The LDIF modify DN change record to process.
   * @param  controls             The set of controls to include in the request.
   * @param  pool                 The connection pool to use to communicate with
   *                              the directory server.
   * @param  multiUpdateRequests  The list to which the request should be added
   *                              if it is to be processed as part of a
   *                              multi-update operation.  It may be
   *                              {@code null} if the operation should not be
   *                              processed via the multi-update operation.
   * @param  rejectWriter         The LDIF writer to use for recording
   *                              information about rejected changes.  It may be
   *                              {@code null} if no reject writer is
   *                              configured.
   *
   * @return  The result code obtained from processing.
   *
   * @throws  LDAPException  If the operation did not complete successfully
   *                         and processing should not continue.
   */
  private ResultCode doModifyDN(final LDIFModifyDNChangeRecord changeRecord,
                                final List<Control> controls,
                                final LDAPConnectionPool pool,
                                final List<LDAPRequest> multiUpdateRequests,
                                final LDIFWriter rejectWriter)
          throws LDAPException
  {
    // Create the modify DN request to process.
    final ModifyDNRequest modifyDNRequest =
         changeRecord.toModifyDNRequest(true);
    for (final Control c : controls)
    {
      modifyDNRequest.addControl(c);
    }


    // If the operation should be processed in a multi-update operation, then
    // just add the request to the list and return without doing anything else.
    if (multiUpdateErrorBehavior.isPresent())
    {
      multiUpdateRequests.add(modifyDNRequest);
      commentToOut(INFO_LDAPMODIFY_MODIFY_DN_ADDED_TO_MULTI_UPDATE.get(
           modifyDNRequest.getDN()));
      return ResultCode.SUCCESS;
    }


    // Try to determine the new DN that the entry will have after the operation.
    DN newDN = null;
    try
    {
      newDN = changeRecord.getNewDN();
    }
    catch (final Exception e)
    {
      Debug.debugException(e);

      // This should only happen if the provided DN, new RDN, or new superior DN
      // was malformed.  Although we could reject the operation now, we'll go
      // ahead and send the request to the server in case it has some special
      // handling for the DN.
    }


    // If the --dryRun argument was provided, then we'll stop here.
    if (dryRun.isPresent())
    {
      if (modifyDNRequest.getNewSuperiorDN() == null)
      {
        if (newDN == null)
        {
          commentToOut(INFO_LDAPMODIFY_DRY_RUN_RENAME.get(
               modifyDNRequest.getDN()));
        }
        else
        {
          commentToOut(INFO_LDAPMODIFY_DRY_RUN_RENAME_TO.get(
               modifyDNRequest.getDN(), newDN.toString()));
        }
      }
      else
      {
        if (newDN == null)
        {
          commentToOut(INFO_LDAPMODIFY_DRY_RUN_MOVE.get(
               modifyDNRequest.getDN()));
        }
        else
        {
          commentToOut(INFO_LDAPMODIFY_DRY_RUN_MOVE_TO.get(
               modifyDNRequest.getDN(), newDN.toString()));
        }
      }
      return ResultCode.SUCCESS;
    }


    // Process the modify DN operation and get the result.
    final String currentDN = modifyDNRequest.getDN();
    if (modifyDNRequest.getNewSuperiorDN() == null)
    {
      if (newDN == null)
      {
        commentToOut(INFO_LDAPMODIFY_MOVING_ENTRY.get(currentDN));
      }
      else
      {
        commentToOut(INFO_LDAPMODIFY_MOVING_ENTRY_TO.get(currentDN,
             newDN.toString()));
      }
    }
    else
    {
      if (newDN == null)
      {
        commentToOut(INFO_LDAPMODIFY_RENAMING_ENTRY.get(currentDN));
      }
      else
      {
        commentToOut(INFO_LDAPMODIFY_RENAMING_ENTRY_TO.get(currentDN,
             newDN.toString()));
      }
    }

    if (verbose.isPresent())
    {
      for (final String ldifLine :
           modifyDNRequest.toLDIFChangeRecord().toLDIF(WRAP_COLUMN))
      {
        out(ldifLine);
      }
      out();
    }


    LDAPResult modifyDNResult;
    try
    {
      modifyDNResult = pool.modifyDN(modifyDNRequest);
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      modifyDNResult = le.toLDAPResult();
    }


    // Display information about the result.
    displayResult(modifyDNResult, useTransaction.isPresent());


    // See if the modify DN operation succeeded or failed.  If it failed, and we
    // should end all processing, then throw an exception.
    switch (modifyDNResult.getResultCode().intValue())
    {
      case ResultCode.SUCCESS_INT_VALUE:
      case ResultCode.NO_OPERATION_INT_VALUE:
        break;

      case ResultCode.ASSERTION_FAILED_INT_VALUE:
        writeRejectedChange(rejectWriter,
             INFO_LDAPMODIFY_ASSERTION_FAILED.get(modifyDNRequest.getDN(),
                  String.valueOf(assertionFilter.getValue())),
             modifyDNRequest.toLDIFChangeRecord(), modifyDNResult);
        throw new LDAPException(modifyDNResult);

      default:
        writeRejectedChange(rejectWriter, null,
             modifyDNRequest.toLDIFChangeRecord(), modifyDNResult);
        if (useTransaction.isPresent() || (! continueOnError.isPresent()))
        {
          throw new LDAPException(modifyDNResult);
        }
        break;
    }

    return modifyDNResult.getResultCode();
  }



  /**
   * Displays information about the provided result, including special
   * processing for a number of supported response controls.
   *
   * @param  result         The result to examine.
   * @param  inTransaction  Indicates whether the operation is part of a
   *                        transaction.
   */
  void displayResult(final LDAPResult result, final boolean inTransaction)
  {
    final ArrayList<String> resultLines = new ArrayList<String>(10);
    ResultUtils.formatResult(resultLines, result, true, inTransaction, 0,
         WRAP_COLUMN);

    if (result.getResultCode() == ResultCode.SUCCESS)
    {
      for (final String line : resultLines)
      {
        out(line);
      }
      out();
    }
    else
    {
      for (final String line : resultLines)
      {
        err(line);
      }
      err();
    }
  }



  /**
   * Writes a line-wrapped, commented version of the provided message to
   * standard output.
   *
   * @param  message  The message to be written.
   */
  private void commentToOut(final String message)
  {
    for (final String line : StaticUtils.wrapLine(message, WRAP_COLUMN - 2))
    {
      out("# ", line);
    }
  }



  /**
   * Writes a line-wrapped, commented version of the provided message to
   * standard error.
   *
   * @param  message  The message to be written.
   */
  private void commentToErr(final String message)
  {
    for (final String line : StaticUtils.wrapLine(message, WRAP_COLUMN - 2))
    {
      err("# ", line);
    }
  }



  /**
   * Writes information about the rejected change to the reject writer.
   *
   * @param  writer        The LDIF writer to which the information should be
   *                       written.  It may be {@code null} if no reject file is
   *                       configured.
   * @param  comment       The comment to include before the change record, in
   *                       addition to the comment generated from the provided
   *                       LDAP result.  It may be {@code null} if no additional
   *                       comment should be included.
   * @param  changeRecord  The LDIF change record to be written.  It must not
   *                       be {@code null}.
   * @param  ldapResult    The LDAP result for the failed operation.  It must
   *                       not be {@code null}.
   */
  private void writeRejectedChange(final LDIFWriter writer,
                                   final String comment,
                                   final LDIFChangeRecord changeRecord,
                                   final LDAPResult ldapResult)
  {
    if (writer == null)
    {
      return;
    }


    final StringBuilder buffer = new StringBuilder();
    if (comment != null)
    {
      buffer.append(comment);
      buffer.append(StaticUtils.EOL);
      buffer.append(StaticUtils.EOL);
    }

    final ArrayList<String> resultLines = new ArrayList<String>(10);
    ResultUtils.formatResult(resultLines, ldapResult, false, false, 0, 0);
    for (final String resultLine : resultLines)
    {
      buffer.append(resultLine);
      buffer.append(StaticUtils.EOL);
    }

    writeRejectedChange(writer, buffer.toString(), changeRecord);
  }



  /**
   * Writes information about the rejected change to the reject writer.
   *
   * @param  writer        The LDIF writer to which the information should be
   *                       written.  It may be {@code null} if no reject file is
   *                       configured.
   * @param  comment       The comment to include before the change record.  It
   *                       may be {@code null} if no comment should be included.
   * @param  changeRecord  The LDIF change record to be written.  It may be
   *                       {@code null} if only a comment should be written.
   */
  void writeRejectedChange(final LDIFWriter writer, final String comment,
                           final LDIFChangeRecord changeRecord)
  {
    if (writer == null)
    {
      return;
    }

    if (rejectWritten.compareAndSet(false, true))
    {
      try
      {
        writer.writeVersionHeader();
      }
      catch (final Exception e)
      {
        Debug.debugException(e);
      }
    }

    try
    {
      if (comment != null)
      {
        writer.writeComment(comment, true, false);
      }

      if (changeRecord != null)
      {
        writer.writeChangeRecord(changeRecord);
      }
    }
    catch (final Exception e)
    {
      Debug.debugException(e);

      commentToErr(ERR_LDAPMODIFY_UNABLE_TO_WRITE_REJECTED_CHANGE.get(
           rejectFile.getValue().getAbsolutePath(),
           StaticUtils.getExceptionMessage(e)));
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void handleUnsolicitedNotification(final LDAPConnection connection,
                                            final ExtendedResult notification)
  {
    final ArrayList<String> lines = new ArrayList<String>(10);
    ResultUtils.formatUnsolicitedNotification(lines, notification, true, 0,
         WRAP_COLUMN);
    for (final String line : lines)
    {
      err(line);
    }
    err();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public LinkedHashMap<String[],String> getExampleUsages()
  {
    final LinkedHashMap<String[],String> examples =
         new LinkedHashMap<String[],String>(2);

    final String[] args1 =
    {
      "--hostname", "ldap.example.com",
      "--port", "389",
      "--bindDN", "uid=admin,dc=example,dc=com",
      "--bindPassword", "password",
      "--defaultAdd"
    };
    examples.put(args1, INFO_LDAPMODIFY_EXAMPLE_1.get());

    final String[] args2 =
    {
      "--hostname", "ds1.example.com",
      "--port", "636",
      "--hostname", "ds2.example.com",
      "--port", "636",
      "--useSSL",
      "--bindDN", "uid=admin,dc=example,dc=com",
      "--bindPassword", "password",
      "--filename", "changes.ldif",
      "--modifyEntriesMatchingFilter", "(objectClass=person)",
      "--searchPageSize", "100"
    };
    examples.put(args2, INFO_LDAPMODIFY_EXAMPLE_2.get());

    return examples;
  }
}
