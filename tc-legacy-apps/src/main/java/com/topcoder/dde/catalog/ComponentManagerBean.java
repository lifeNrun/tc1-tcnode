/*
 * Copyright (C) 2004 - 2009 TopCoder Inc., All Rights Reserved.
 */
package com.topcoder.dde.catalog;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.ObjectNotFoundException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.sql.DataSource;

import com.cronos.termsofuse.dao.ProjectTermsOfUseDao;
import com.cronos.termsofuse.dao.TermsOfUsePersistenceException;
import com.jivesoftware.base.UnauthorizedException;
import com.jivesoftware.base.UserNotFoundException;
import com.jivesoftware.forum.ForumCategoryNotFoundException;
import com.topcoder.apps.review.document.DocumentManagerHome;
import com.topcoder.apps.review.projecttracker.ProjectTrackerV2;
import com.topcoder.apps.review.projecttracker.ProjectTrackerV2Home;
import com.topcoder.apps.review.projecttracker.ProjectType;
import com.topcoder.apps.review.projecttracker.User;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDECategories;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDECategoriesHome;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDECompCatalog;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDECompCatalogHome;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDECompCategories;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDECompCategoriesHome;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDECompDependencies;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDECompDependenciesHome;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDECompDocumentation;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDECompDocumentationHome;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDECompDownload;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDECompDownloadHome;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDECompExamples;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDECompExamplesHome;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDECompForumXref;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDECompForumXrefHome;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDECompKeywords;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDECompKeywordsHome;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDECompReviews;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDECompReviewsHome;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDECompTechnology;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDECompTechnologyHome;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDECompVersionDates;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDECompVersionDatesHistoryHome;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDECompVersionDatesHome;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDECompVersions;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDECompVersionsHome;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDEDownloadTrackingHome;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDELicenseLevel;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDELicenseLevelHome;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDERolesHome;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDETechnologyTypes;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDETechnologyTypesHome;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDEUserMaster;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDEUserMasterHome;
import com.topcoder.dde.persistencelayer.interfaces.LocalDDEUserRoleHome;
import com.topcoder.security.GeneralSecurityException;
import com.topcoder.security.RolePrincipal;
import com.topcoder.security.TCSubject;
import com.topcoder.security.admin.PolicyMgrRemote;
import com.topcoder.security.admin.PolicyMgrRemoteHome;
import com.topcoder.security.admin.PrincipalMgrRemote;
import com.topcoder.security.admin.PrincipalMgrRemoteHome;
import com.topcoder.security.policy.PermissionCollection;
import com.topcoder.security.policy.PolicyRemote;
import com.topcoder.security.policy.PolicyRemoteHome;
import com.topcoder.shared.util.ApplicationServer;
import com.topcoder.shared.util.TCContext;
import com.topcoder.util.config.ConfigManager;
import com.topcoder.util.config.ConfigManagerException;
import com.topcoder.util.config.ConfigManagerInterface;
import com.topcoder.util.errorhandling.BaseException;
import com.topcoder.web.common.TermsOfUseUtil;
import com.topcoder.web.ejb.forums.Forums;
import com.topcoder.web.ejb.forums.ForumsHome;
import com.topcoder.web.ejb.userservices.UserServices;
import com.topcoder.web.ejb.userservices.UserServicesLocator;

/**
 * The implementation of the methods of ComponentManagerEJB.
 *
 * <p>
 * Version 1.0.1 Change notes:
 * <ol>
 * <li>
 * Class was updated to deal with the new user_role description attribute.
 * </li>
 * <li>
 * Class was updated to deal with the elimination of tcsrating attribute.
 * </li>
 * </ol>
 * </p>
 * <p>
 * Version 1.0.2 Change notes:
 * <ol>
 * <li>
 * Version admin tool was fixed to allow post-creation public forum flag changes.
 * </li>
 * </p>
 * <p>
 * Version 1.0.3 (Configurable Contest Terms Release Assembly v1.0) Change notes:
 * <ol>
 * <li>Added Project Role User Terms Of Use association generation to project creation.</li>
 * </ol>
 * </p>
 * <p>
 * Version 1.0.4 (Configurable Contest Terms Release Assembly v2.0) Change notes:
 * <ol>
 * <li>Added sort order parameter to project role terms of use service call.</li>
 * </ol>
 * </p>
 *
 *
 * @author Albert Mao, pulky
 * @version 1.0.4
 * @see ComponentManager
 * @see ComponentManagerHome
 */
public class ComponentManagerBean
        implements SessionBean, ConfigManagerInterface {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ComponentManagerBean.class);

    /**
     * Default sort order for project role terms of use generation
     * 
     * @since 1.0.4
     */
    private static final int DEFAULT_TERMS_SORT_ORDER = 1;
    
    private static final String
            CONFIG_NAMESPACE = "com.topcoder.dde.catalog.ComponentManagerBean";

    private static final int[] competitionCategories = {1, 2, 6, 7, 13, 14, 16, 17, 18, 19, 20, 21, 23};

    //Permission constants
    private static final long JAVA_PERM = 21;
    private static final long NET_PERM = 22;
    private static final long JAVA_CAT = 5801776;
    private static final long NET_CAT = 5801777;

    /*
     * The following field declarations should be private, but a bug in JBoss
     * prevents stateful session beans from being passivated and activated
     * unless all these fields are public. When this bug in JBoss is fixed, the
     * following fields should be made private.
     */
    public SessionContext ejbContext;
    public LocalDDECompCatalogHome catalogHome;
    public LocalDDECompVersionsHome versionsHome;
    public LocalDDECompVersionDatesHome versionDatesHome;
    public LocalDDECompVersionDatesHistoryHome versionDatesHistoryHome;
    public LocalDDECompCategoriesHome compcatsHome;
    public LocalDDECompKeywordsHome keywordsHome;
    public LocalDDECompTechnologyHome comptechHome;
    public LocalDDECompDocumentationHome docHome;
    public LocalDDECompExamplesHome exampleHome;
    public LocalDDECompDownloadHome downloadHome;
    public LocalDDECompDependenciesHome depHome;
    public LocalDDECompReviewsHome reviewHome;
    public LocalDDECompForumXrefHome compforumHome;
    public LocalDDECategoriesHome categoriesHome;
    public LocalDDETechnologyTypesHome technologiesHome;
    public LocalDDERolesHome rolesHome;
    public LocalDDEUserRoleHome userroleHome;
    public LocalDDEUserMasterHome userHome;
    public LocalDDEDownloadTrackingHome trackingHome;
    public LocalDDELicenseLevelHome licenseHome;
    public PrincipalMgrRemoteHome principalmgrHome;
    public PolicyMgrRemoteHome policymgrHome;
    public PolicyRemoteHome policyHome;
    public ProjectTrackerV2Home projectTrackerHome;
    public DocumentManagerHome documentManagerHome;
    public long componentId;
    public long versionId;
    public long version;


    public ComponentManagerBean() {
    }


    private void lookupInterfaces() throws NamingException {
        Context homeBindings = new InitialContext();

        catalogHome = (LocalDDECompCatalogHome)
                homeBindings.lookup(LocalDDECompCatalogHome.EJB_REF_NAME);
        versionsHome = (LocalDDECompVersionsHome)
                homeBindings.lookup(LocalDDECompVersionsHome.EJB_REF_NAME);

        versionDatesHome = (LocalDDECompVersionDatesHome)
                homeBindings.lookup(LocalDDECompVersionDatesHome.EJB_REF_NAME);

        versionDatesHistoryHome = (LocalDDECompVersionDatesHistoryHome)
                homeBindings.lookup(LocalDDECompVersionDatesHistoryHome.EJB_REF_NAME);


        compcatsHome = (LocalDDECompCategoriesHome)
                homeBindings.lookup(LocalDDECompCategoriesHome.EJB_REF_NAME);
        keywordsHome = (LocalDDECompKeywordsHome)
                homeBindings.lookup(LocalDDECompKeywordsHome.EJB_REF_NAME);
        comptechHome = (LocalDDECompTechnologyHome)
                homeBindings.lookup(LocalDDECompTechnologyHome.EJB_REF_NAME);
        docHome = (LocalDDECompDocumentationHome)
                homeBindings.lookup(LocalDDECompDocumentationHome.EJB_REF_NAME);
        exampleHome = (LocalDDECompExamplesHome)
                homeBindings.lookup(LocalDDECompExamplesHome.EJB_REF_NAME);
        downloadHome = (LocalDDECompDownloadHome)
                homeBindings.lookup(LocalDDECompDownloadHome.EJB_REF_NAME);
        depHome = (LocalDDECompDependenciesHome)
                homeBindings.lookup(LocalDDECompDependenciesHome.EJB_REF_NAME);
        reviewHome = (LocalDDECompReviewsHome)
                homeBindings.lookup(LocalDDECompReviewsHome.EJB_REF_NAME);
        compforumHome = (LocalDDECompForumXrefHome)
                homeBindings.lookup(LocalDDECompForumXrefHome.EJB_REF_NAME);
        categoriesHome = (LocalDDECategoriesHome)
                homeBindings.lookup(LocalDDECategoriesHome.EJB_REF_NAME);
        technologiesHome = (LocalDDETechnologyTypesHome)
                homeBindings.lookup(LocalDDETechnologyTypesHome.EJB_REF_NAME);
        rolesHome = (LocalDDERolesHome)
                homeBindings.lookup(LocalDDERolesHome.EJB_REF_NAME);
        userroleHome = (LocalDDEUserRoleHome)
                homeBindings.lookup(LocalDDEUserRoleHome.EJB_REF_NAME);
        userHome = (LocalDDEUserMasterHome)
                homeBindings.lookup(LocalDDEUserMasterHome.EJB_REF_NAME);
        trackingHome = (LocalDDEDownloadTrackingHome)
                homeBindings.lookup(LocalDDEDownloadTrackingHome.EJB_REF_NAME);
        licenseHome = (LocalDDELicenseLevelHome)
                homeBindings.lookup(LocalDDELicenseLevelHome.EJB_REF_NAME);
/*
            /** SECURITY MANAGER
        Hashtable principalMgrEnvironment=new Hashtable();
        principalMgrEnvironment.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
        try{
            principalMgrEnvironment.put(Context.PROVIDER_URL, getConfigValue("securitymanagerip"));
        } catch(ConfigManagerException exception) {
            throw new NamingException(
            "Unable to access configuration file: " + exception.toString());
        }


        Context principalMgrContext = new InitialContext(principalMgrEnvironment);
  */
        principalmgrHome = (PrincipalMgrRemoteHome) PortableRemoteObject.narrow(
                homeBindings.lookup(PrincipalMgrRemoteHome.EJB_REF_NAME),
                PrincipalMgrRemoteHome.class);
        policymgrHome = (PolicyMgrRemoteHome) PortableRemoteObject.narrow(
                homeBindings.lookup(PolicyMgrRemoteHome.EJB_REF_NAME),
                PolicyMgrRemoteHome.class);
        policyHome = (PolicyRemoteHome)
                PortableRemoteObject.narrow(
                        homeBindings.lookup(PolicyRemoteHome.EJB_REF_NAME),
                        PolicyRemoteHome.class);

        // Online Review
        projectTrackerHome = (ProjectTrackerV2Home) PortableRemoteObject.narrow(
                homeBindings.lookup(ProjectTrackerV2Home.EJB_REF_NAME),
                ProjectTrackerV2Home.class);
        documentManagerHome = (DocumentManagerHome) PortableRemoteObject.narrow(
                homeBindings.lookup(DocumentManagerHome.EJB_REF_NAME),
                DocumentManagerHome.class);
    }

    public void ejbCreate(long componentId) throws CreateException {
        try {
            lookupInterfaces();
        } catch (NamingException exception) {
            throw new EJBException(exception.toString());
        }
        LocalDDECompCatalog targetComp;
        try {
            targetComp =
                    catalogHome.findByPrimaryKey(new Long(componentId));
        } catch (ObjectNotFoundException exception) {
            throw new CreateException(
                    "Specified component does not exist in catalog: "
                            + exception.toString());
        } catch (FinderException exception) {
            throw new CreateException(exception.toString());
        }
        this.componentId = componentId;
        this.version = targetComp.getCurrentVersion();
        try {
            this.versionId = ((Long) versionsHome.
                    findByComponentIdAndVersion(componentId, version).
                    getPrimaryKey()).longValue();

        } catch (FinderException exception) {
            throw new CreateException(exception.toString());
        }
    }

    public void ejbCreate() throws CreateException {
        try {
            lookupInterfaces();
        } catch (NamingException exception) {
            throw new EJBException(exception.toString());
        }
    }

    public void ejbCreate(long componentId, long version)
            throws CreateException {
        try {
            lookupInterfaces();
        } catch (NamingException exception) {
            throw new EJBException(exception.toString());
        }
        try {
            LocalDDECompCatalog targetComp =
                    catalogHome.findByPrimaryKey(new Long(componentId));
        } catch (ObjectNotFoundException exception) {
            throw new CreateException(
                    "Specified component does not exist in the catalog: "
                            + exception.toString());
        } catch (FinderException exception) {
            throw new CreateException(exception.toString());
        }
        this.componentId = componentId;
        this.version = version;
        try {
            this.versionId = ((Long) versionsHome.
                    findByComponentIdAndVersion(componentId, version).
                    getPrimaryKey()).longValue();
        } catch (ObjectNotFoundException exception) {
            throw new CreateException(
                    "Specified version does not exist in the catalog: "
                            + exception.toString());
        } catch (FinderException exception) {
            throw new CreateException(exception.toString());
        }
    }

    public void setRootCategory(long rootCategory) throws RemoteException, CatalogException {

        PermissionCollection perms = null;
        RolePrincipal role = null;
        RolePrincipal catalogRole = null;
        RolePrincipal oldCatalogRole = null;
        try {
            LocalDDECompCatalog comp = catalogHome.findByPrimaryKey(new Long(componentId));
            comp.setRootCategory(rootCategory);

            PrincipalMgrRemote principalManager = principalmgrHome.create();
            PolicyMgrRemote policyManager = policymgrHome.create();

            catalogRole = principalManager.getRole(JAVA_CAT == rootCategory ? JAVA_PERM : NET_PERM);
            oldCatalogRole = principalManager.getRole(JAVA_CAT == rootCategory ? NET_PERM : JAVA_PERM);

            //I don't think you need the line below since it will be created already
            //role = principalManager.createRole("DDEComponentDownload " + componentId, null);

            perms = new PermissionCollection();
            perms.addPermission(new DownloadPermission(componentId));
            policyManager.addPermissions(catalogRole, perms, null);
            perms = new PermissionCollection();
            //perms.addPermission(new DownloadPermission(componentId));
            //policyManager.removePermissions(oldCatalogRole, perms, null);

        } catch (CreateException exception) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(
                    "Failed to create security role for component: "
                            + exception.toString());
        } catch (GeneralSecurityException exception) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(
                    "Failed to create security role for component: "
                            + exception.toString());
        } catch (RemoteException exception) {
            throw new EJBException(exception.toString());
        } catch (FinderException e) {
            throw new CatalogException(e.toString());
        }
    }

    public long getRootCategory() throws RemoteException, CatalogException {

        try {
            LocalDDECompCatalog comp = catalogHome.findByPrimaryKey(new Long(componentId));
            return comp.getRootCategory();
        } catch (FinderException e) {
            throw new CatalogException(e.toString());
        }
    }


    public void setVersion(long version) throws CatalogException {
        try {
            LocalDDECompVersions newVer =
                    versionsHome.findByComponentIdAndVersion(componentId, version);
            this.versionId = ((Long) newVer.getPrimaryKey()).longValue();
            this.version = version;
        } catch (ObjectNotFoundException exception) {
            throw new CatalogException(
                    "Specified version does not exist in the catalog: "
                            + exception.toString());
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }
    }

    public long getNumVersions() throws CatalogException {
        LocalDDECompCatalog comp;
        try {
            comp = catalogHome.findByPrimaryKey(new Long(componentId));
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }
        long number;
        try {
            number = versionsHome.findByComponentId(
                    ((Long) comp.getPrimaryKey()).longValue()).size();
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }
        return number;
    }


    public ComponentInfo getComponentInfo() throws CatalogException {
        try {
            StringBuffer keywords = new StringBuffer();
            Iterator wordIterator =
                    keywordsHome.findByComponentId(componentId).iterator();
            while (wordIterator.hasNext()) {
                LocalDDECompKeywords wordBean =
                        (LocalDDECompKeywords) wordIterator.next();
                keywords.append(wordBean.getKeyword());
                keywords.append(ComponentInfo.KEYWORD_DELIMITER);
            }

            LocalDDECompCatalog compBean =
                    catalogHome.findByPrimaryKey(new Long(componentId));
            return new ComponentInfo(componentId, compBean.getCurrentVersion(),
                    compBean.getComponentName(), compBean.getShortDesc(),
                    compBean.getDescription(), compBean.getFunctionDesc(),
                    keywords.toString(), compBean.getStatusId());
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }
    }

    private ComponentVersionInfo generateInfo(LocalDDECompVersions bean) throws CatalogException {
        /*
         * The version text must be trim()ed because the database currently
         * stores it as a fixed-length string.  The trim() should be removed
         * once this is corrected.
         */

        ComponentVersionInfo cvi = new ComponentVersionInfo(
                ((Long) bean.getPrimaryKey()).longValue(),
                bean.getVersion(), bean.getVersionText().trim(),
                bean.getComments(), bean.getPhaseId(),
                new Date(bean.getPhaseTime().getTime()), bean.getPrice(), bean.getSuspended());

        Forums forums = null;
        try {
            Context context = TCContext.getInitial(ApplicationServer.FORUMS_HOST_URL);
            ForumsHome forumsHome = (ForumsHome) context.lookup(ForumsHome.EJB_REF_NAME);
            forums = forumsHome.create();
        } catch (NamingException e) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(
                    "Failed to connect to forums server EJB: "
                            + e.toString());
        } catch (CreateException e) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(e.toString());
        } catch (RemoteException e) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(e.toString());
        }

        // look for the public forum flag
        try {
            long categoryId = 0;
            versionId = ((Long) bean.getPrimaryKey()).longValue();
            log.debug("versionId: " + versionId);

            Iterator forumIterator;
            try {
                forumIterator = compforumHome.findByCompVersId(versionId).iterator();
            } catch (FinderException impossible) {
                throw new CatalogException("Could not find forum: " + impossible.toString());
            }
            if (forumIterator.hasNext()) {
                categoryId = ((LocalDDECompForumXref) forumIterator.next()).getCategoryId();
                cvi.setPublicForum(forums.isPublic(categoryId));
            }
        } catch (UnauthorizedException exception) {
            throw new CatalogException(
                    "Not authorized to determine public forums: " + exception.toString());
        } catch (ForumCategoryNotFoundException exception) {
            log.error("error", exception);
            throw new CatalogException(
                    "Forum category not found: " + exception.toString());
        } catch (RemoteException exception) {
            throw new CatalogException(exception.toString());
        } catch (EJBException exception) {
            throw new CatalogException(exception.toString());
        }

        return cvi;
    }

    private VersionDateInfo generateVersionDateInfo(LocalDDECompVersionDates bean) {
        /*
         * The version text must be trim()ed because the database currently
         * stores it as a fixed-length string.  The trim() should be removed
         * once this is corrected.
         */
        Date estimatedDevDate = null;
        if (bean.getEstimatedDevDate() != null) {
            estimatedDevDate = new Date(bean.getEstimatedDevDate().getTime());
        }
        Date screeningCompleteDate = null;
        if (bean.getScreeningCompleteDate() != null) {
            screeningCompleteDate = new Date(bean.getScreeningCompleteDate().getTime());
        }
        Date phaseCompleteDate = null;
        if (bean.getPhaseCompleteDate() != null) {
            phaseCompleteDate = new Date(bean.getPhaseCompleteDate().getTime());
        }
        Date aggregationCompleteDate = null;
        if (bean.getAggregationCompleteDate() != null) {
            aggregationCompleteDate = new Date(bean.getAggregationCompleteDate().getTime());
        }
        Date reviewCompleteDate = null;
        if (bean.getReviewCompleteDate() != null) {
            reviewCompleteDate = new Date(bean.getReviewCompleteDate().getTime());
        }
        Date productionDate = null;
        if (bean.getProductionDate() != null) {
            productionDate = new Date(bean.getProductionDate().getTime());
        }

        return new VersionDateInfo(
                ((Long) bean.getPrimaryKey()).longValue(),
                bean.getComponentVersionId(),
                bean.getPhaseId(),
                new Date(bean.getPostingDate().getTime()),
                new Date(bean.getInitialSubmissionDate().getTime()),
                new Date(bean.getFinalSubmissionDate().getTime()),
                new Date(bean.getWinnerAnnouncedDate().getTime()),
                estimatedDevDate,
                bean.getPrice(),
                bean.getStatusId(), bean.getLevelId(),
                screeningCompleteDate, phaseCompleteDate,
                aggregationCompleteDate, reviewCompleteDate,
                bean.getPhaseCompleteDateComment(),
                bean.getAggregationCompleteDateComment(),
                bean.getReviewCompleteDateComment(),
                bean.getScreeningCompleteDateComment(),
                bean.getInitialSubmissionDateComment(),
                bean.getFinalSubmissionDateComment(),
                bean.getWinnerAnnouncedDateComment(),
                productionDate, bean.getProductionDateComment());


    }


    public ComponentVersionInfo getVersionInfo() throws CatalogException {
        try {
            LocalDDECompVersions versionBean =
                    versionsHome.findByPrimaryKey(new Long(versionId));
            return generateInfo(versionBean);
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }
    }

    public VersionDateInfo getVersionDateInfo(long componentVersionId, long phaseId) throws CatalogException {
        try {
            LocalDDECompVersionDates versionDatesBean =
                    versionDatesHome.findByComponentVersionId(componentVersionId, phaseId);
            return generateVersionDateInfo(versionDatesBean);
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }
    }


    public Collection getAllVersionInfo() throws CatalogException {
        List info = new ArrayList();
        Iterator versionIterator;
        try {
            versionIterator =
                    versionsHome.findByComponentId(componentId).iterator();
        } catch (FinderException impossible) {
            throw new CatalogException(impossible.toString());
        }
        while (versionIterator.hasNext()) {
            LocalDDECompVersions versionBean = (LocalDDECompVersions)
                    versionIterator.next();
            info.add(generateInfo(versionBean));
        }
        Collections.sort(info, new Comparators.VersionSorter());
        return info;
    }

    public long createNewVersion(ComponentVersionRequest request)
            throws CatalogException {
        if (request == null) {
            throw new CatalogException(
                    "Null specified for version request");
        }

        LocalDDECompCatalog comp;
        try {
            comp = catalogHome.findByPrimaryKey(new Long(componentId));
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }

        int latest;
        try {
            latest = versionsHome.findByComponentId(
                    ((Long) comp.getPrimaryKey()).longValue()).size();
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }
        if (latest == Integer.MAX_VALUE) {
            throw new CatalogException(// It could happen!
                    "Component has enough versions already. Get a life.");
        }
        long nextVersion = latest + 1;
        Timestamp currentTime = new Timestamp((new Date()).getTime());

        LocalDDECompVersions newVer;
        try {
            newVer = versionsHome.create(
                    nextVersion, // version number
                    currentTime, // create time
                    ComponentVersionInfo.COLLABORATION, // phase
                    currentTime, // phase time
                    0.0, // price
                    request.getComments(), // version comments
                    comp, // component bean reference
                    request.getVersionLabel(), // version label
                    false       // suspended
            );
        } catch (CreateException exception) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(exception.toString());
        }

        try {
            Iterator techIterator = request.getTechnologies().iterator();
            while (techIterator.hasNext())
                comptechHome.create(newVer, technologiesHome.
                        findByPrimaryKey((Long) techIterator.next()));
        } catch (ObjectNotFoundException exception) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(
                    "Specified technology does not exist in the catalog: "
                            + exception.toString());
        } catch (FinderException exception) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(exception.toString());
        } catch (CreateException exception) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(exception.toString());
        }

        return nextVersion;
    }

    public void updateComponentInfo(ComponentInfo info)
            throws CatalogException {
        if (info == null) {
            throw new CatalogException(
                    "Null specified for component info");
        }
        if (info.getId() != componentId) {
            throw new CatalogException(
                    "Specified component is not managed by this instance");
        }

        LocalDDECompCatalog compBean;
        try {
            compBean = catalogHome.findByPrimaryKey(new Long(componentId));
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }

        // Change Online Review Security roles on component name change
        if (!compBean.getComponentName().equals(info.getName())) {
            try {
                ProjectTrackerV2 pt = projectTrackerHome.create();
                pt.componentRename(componentId,
                        compBean.getComponentName(),
                        info.getName());
            } catch (RemoteException e) {
                ejbContext.setRollbackOnly();
                throw new CatalogException(e.toString());
            } catch (CreateException e) {
                ejbContext.setRollbackOnly();
                throw new CatalogException(e.toString());
            }
        }

        compBean.setComponentName(info.getName());
        compBean.setShortDesc(info.getShortDescription());
        compBean.setDescription(info.getDescription());
        compBean.setFunctionDesc(info.getFunctionalDescription());
        compBean.setStatusId(info.getStatus());


        Iterator oldIterator;
        try {
            oldIterator =
                    keywordsHome.findByComponentId(componentId).iterator();
        } catch (FinderException exception) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(exception.toString());
        }
        try {
            while (oldIterator.hasNext())
                ((LocalDDECompKeywords) oldIterator.next()).remove();
        } catch (RemoveException exception) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(exception.toString());
        }

        StringTokenizer newWords = new StringTokenizer(info.getKeywords(),
                ComponentInfo.KEYWORD_DELIMITER);
        try {
            while (newWords.hasMoreTokens())
                keywordsHome.create(newWords.nextToken(), compBean);
        } catch (CreateException exception) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(exception.toString());
        }
    }

    /**
     * This method will process version update requests.
     *
     * Note - Configurable Contest Terms Release Assembly v1.0: added Project Role Terms of Use generation.
     *
     * @param info the ComponentVersionInfo to update
     * @param requestor the requestor
     * @param levelId the levelId
     * @throws CatalogException if any error occurs
     */
    public void updateVersionInfo(ComponentVersionInfo info, TCSubject requestor, long levelId)
            throws CatalogException {

        log.debug("public: " + info.getPublicForum());


        if (info == null) {
            throw new CatalogException(
                    "Null specified for version info");
        }
        if (info.getVersionId() != versionId) {
            throw new CatalogException(
                    "Specified version info does refer to the active version");
        }
        LocalDDECompVersions versionBean;
        try {
            versionBean =
                    versionsHome.findByPrimaryKey(new Long(info.getVersionId()));
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }

        Forums forumsBean = getForumsBean();

        // If the version is completed:
        if (info.getPhase() == ComponentVersionInfo.COMPLETED
                && versionBean.getPhaseId() != info.getPhase()) {
            LocalDDECompCatalog compBean;
            try {
                compBean = catalogHome.findByPrimaryKey(new Long(componentId));
            } catch (FinderException exception) {
                throw new CatalogException(exception.toString());
            }
            Iterator forumIterator;
            try {
                forumIterator =
                        compforumHome.findByCompVersId(versionId).iterator();
            } catch (FinderException impossible) {
                ejbContext.setRollbackOnly();
                throw new CatalogException(impossible.toString());
            }
            try {
                while (forumIterator.hasNext()) {
                    LocalDDECompForumXref forumRef =
                            (LocalDDECompForumXref) forumIterator.next();
                    forumsBean.closeCategory(forumRef.getCategoryId());
                }
            } catch (RemoteException exception) {
                ejbContext.setRollbackOnly();
                throw new CatalogException(exception.toString());
            } catch (UnauthorizedException exception) {
                throw new CatalogException(
                        "Not authorized to determine public forums: " + exception.toString());
            } catch (ForumCategoryNotFoundException exception) {
                throw new CatalogException(
                        "Forum category not found: " + exception.toString());
            }

            if (version > compBean.getCurrentVersion()
                    || compBean.getCurrentVersion() == 1) {
                compBean.setCurrentVersion(version);
                StringBuffer indexDigest = new StringBuffer();
                ComponentInfo compInfo = getComponentInfo();
                indexDigest.append(compInfo.getName());
                indexDigest.append(CatalogSearchEngine.DELIMITER);
                indexDigest.append(compInfo.getShortDescription());
                indexDigest.append(CatalogSearchEngine.DELIMITER);
                indexDigest.append(compInfo.getDescription());
                indexDigest.append(CatalogSearchEngine.DELIMITER);
                indexDigest.append(compInfo.getFunctionalDescription());
                indexDigest.append(CatalogSearchEngine.DELIMITER);
                indexDigest.append(compInfo.getKeywords());
                indexDigest.append(CatalogSearchEngine.DELIMITER);
                Iterator techIterator = getTechnologies().iterator();
                while (techIterator.hasNext()) {
                    Technology tech = (Technology) techIterator.next();
                    indexDigest.append(tech.getName());
                    indexDigest.append(CatalogSearchEngine.DELIMITER);
                }
                CatalogSearchEngine.getInstance().
                        reIndex(componentId, indexDigest.toString());
            }
        }

        long categoryID = -1;

        // If the version is changing to specification or development
        // Makes sure the specification forum is created even if the component
        // is moved directly from collaboration to development.
        LocalDDECompCatalog compBean;
        try {
            compBean = catalogHome.findByPrimaryKey(new Long(componentId));
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }

        Collection forums;
        try {
            forums = compforumHome.findByCompVersId(versionId);
        } catch (FinderException exception) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(exception.toString());
        }
        if (forums.size() == 0) {
            if (info.getPhase() == ComponentVersionInfo.SPECIFICATION
                    || info.getPhase() == ComponentVersionInfo.DEVELOPMENT) {

                if (!ejbContext.getRollbackOnly()) {
                    try {
                        /*
                               * This should be replaced by a distributed transaction (XA, etc.) that rolls back
                               * changes on the software and forum servers when an error in the workflow occurs.
                               */
                        //log.info("*** [ComponentManagerBean.updateVersionInfo()] calling createSoftwareComponentForums in forums EJB: " + Calendar.getInstance().getTime());
                        categoryID = forumsBean.createSoftwareComponentForums(compBean.getComponentName(),
                                ((Long) compBean.getPrimaryKey()).longValue(), ((Long) versionBean.getPrimaryKey()).longValue(),
                                versionBean.getPhaseId(), compBean.getStatusId(), compBean.getRootCategory(),
                                compBean.getShortDesc(), versionBean.getVersionText(), info.getPublicForum());
                        compforumHome.create(categoryID, info.getVersionId());
                        //log.info("*** [ComponentManagerBean.updateVersionInfo()] finished createSoftwareComponentForums in forums EJB: " + Calendar.getInstance().getTime());
                    } catch (RemoteException e) {
                        ejbContext.setRollbackOnly();
                        throw new CatalogException(e.toString());
                    } catch (Exception e) {
                        ejbContext.setRollbackOnly();
                        throw new CatalogException(e.toString());
                    }
                }
            }
        } else {
            log.debug("Updating public flag");
            // all forums are created, but the public attribute must be updated.
            for (Iterator it = forums.iterator(); it.hasNext();) {
                LocalDDECompForumXref compForumXref = (LocalDDECompForumXref) it.next();

                try {
                    log.debug("Looking for forum category: " + compForumXref.getCategoryId());

                    try {
                        forumsBean.setPublic(compForumXref.getCategoryId(), info.getPublicForum());
                    } catch (UnauthorizedException exception) {
                        throw new CatalogException(
                                "Not authorized to determine public forums: " + exception.toString());
                    } catch (ForumCategoryNotFoundException exception) {
                        throw new CatalogException(
                                "Forum category not found: " + exception.toString());
                    }
                } catch (RemoteException exception) {
                    ejbContext.setRollbackOnly();
                    throw new EJBException(exception.toString());
                }
            }
        }

        // Change Online Review Security roles if versionText has changed
        if (!versionBean.getVersionText().trim().equals(info.getVersionLabel())) {
            try {
                ProjectTrackerV2 pt = projectTrackerHome.create();
                pt.versionRename(info.getVersionId(),
                        versionBean.getVersionText().trim(),
                        info.getVersionLabel());
            } catch (RemoteException e) {
                ejbContext.setRollbackOnly();
                throw new CatalogException(e.toString());
            } catch (CreateException e) {
                ejbContext.setRollbackOnly();
                throw new CatalogException(e.toString());
            }
        }

        // Create Online Review project on phase changes
        if ((versionBean.getPhaseId() == ComponentVersionInfo.COLLABORATION
                && info.getPhase() == ComponentVersionInfo.SPECIFICATION) ||
                (versionBean.getPhaseId() == ComponentVersionInfo.SPECIFICATION
                        && info.getPhase() == ComponentVersionInfo.DEVELOPMENT)) {
            log.debug("Phase Change.");

            long projectTypeId;
            if (info.getPhase() == ComponentVersionInfo.SPECIFICATION) {
                projectTypeId = 1;
            } else {
                projectTypeId = 2;
            }

            // A String description of the notification event corresponding to current component
            String description = null;

            try {
                Context homeBindings = new InitialContext();
                ProjectTrackerV2Home ptHome = (ProjectTrackerV2Home) PortableRemoteObject.narrow(
                        homeBindings.lookup(ProjectTrackerV2Home.EJB_REF_NAME),
                        ProjectTrackerV2Home.class);
                ProjectTrackerV2 pt = ptHome.create();

                // if component went to dev, get the winner from design to add to forum post notification.
                if ((versionBean.getPhaseId() != ComponentVersionInfo.DEVELOPMENT) &&
                        (info.getPhase() == ComponentVersionInfo.DEVELOPMENT)) {
                    log.debug("Project went to development. Design winner will be added to notification");

                    long[] winnerCategoryIds = pt.getProjectWinnerIdForumCategoryId(
                            pt.getProjectIdByComponentVersionId(getVersionInfo().getVersionId(), ProjectType.ID_DESIGN), requestor);

                    if (winnerCategoryIds[0] != 0) {
                        log.debug("WinnerId=" + winnerCategoryIds[0]);
                        try {
                            forumsBean.createCategoryWatch(winnerCategoryIds[0], winnerCategoryIds[1]);
                        } catch (UnauthorizedException exception) {
                            throw new CatalogException(
                                    "Not authorized to determine public forums: " + exception.toString());
                        } catch (ForumCategoryNotFoundException exception) {
                            throw new CatalogException(
                                    "Forum category not found: " + exception.toString());
                        } catch (UserNotFoundException exception) {
                            throw new CatalogException(
                                    "User not found: " + exception.toString());
                        }
                    } else {
                        log.debug("Winner can't be retrieved because project.getWinner()==null.  No notification added");
                    }
                }

                long projectId = pt.createProject(
                        versionBean.getCompCatalog().getComponentName(),
                        info.getVersionLabel(),
                        info.getVersionId(),
                        projectTypeId,
                        versionBean.getCompCatalog().getDescription(),
                        null,
                        requestor,
                        levelId,
                        categoryID,
                        info.getPrice());


                if (categoryID >= 0) {
                    log.debug("New category created, adding PM to notification. New category: " + categoryID);

                    User pm = pt.getPM(projectId);

                    if (pm == null) {
                        log.debug("The PM can't be retrieved for this project.  Notification not added.");
                    } else {
                        try {
                            forumsBean.createCategoryWatch(pm.getId(), categoryID);
                        } catch (UnauthorizedException exception) {
                            throw new CatalogException(
                                    "Not authorized to determine public forums: " + exception.toString());
                        } catch (ForumCategoryNotFoundException exception) {
                            throw new CatalogException(
                                    "Forum category not found: " + exception.toString());
                        } catch (UserNotFoundException exception) {
                            throw new CatalogException(
                                    "User not found: " + exception.toString());
                        }
                    }
                }

                // generate new project role terms of use associations for the recently created project.
                generateProjectRoleTermsOfUseAssociations(projectId, projectTypeId);

            } catch (ConfigManagerException e) {
                ejbContext.setRollbackOnly();
                throw new CatalogException(e.toString());
            } catch (ClassCastException e) {
                ejbContext.setRollbackOnly();
                throw new CatalogException(e.toString());
            } catch (RemoteException e) {
                ejbContext.setRollbackOnly();
                throw new CatalogException(e.toString());
            } catch (NamingException e) {
                ejbContext.setRollbackOnly();
                throw new CatalogException(e.toString());
            } catch (CreateException e) {
                ejbContext.setRollbackOnly();
                throw new CatalogException(e.toString());
            } catch (BaseException e) {
                ejbContext.setRollbackOnly();
                throw new CatalogException(e.getMessage());
            } catch (Exception e) {
                ejbContext.setRollbackOnly();
                throw new CatalogException(e.getMessage());
            }
        }


        versionBean.setVersionText(info.getVersionLabel());
        versionBean.setComments(info.getComments());
        versionBean.setPhaseId(info.getPhase());
        versionBean.setPhaseTime(
                new Timestamp(info.getPhaseDate().getTime()));
        versionBean.setPrice(info.getPrice());
        versionBean.setSuspended(info.getSuspended());
    }


    /**
     * Private helper method to generate default Project Role Terms of Use associations for a given project.
     *
     * @param projectId the project id for the associations
     * @param projectTypeId the project type id of the provided project id
     * @throws NumberFormatException if configurations have wrong format
     * @throws ConfigManagerException if Configuration Manager fails to retrieve the configurations
     * @throws NamingException if any errors occur during EJB lookup
     * @throws RemoteException if any errors occur during EJB remote invocation
     * @throws CreateException if any errors occur during EJB creation
     * @throws EJBException if any other errors occur while invoking EJB services
     * @throws TermsOfUsePersistenceException if nay errors occurs when creating project role terms of use
     * @throws Exception if any other error occurs
     * @since 1.0.3
     */
    private void generateProjectRoleTermsOfUseAssociations(long projectId, long projectTypeId)
            throws NumberFormatException, ConfigManagerException,
            NamingException, RemoteException, CreateException, EJBException, TermsOfUsePersistenceException, Exception {

        // get ProjectRoleTermsOfUse entries configurations
        int submitterRoleId = Integer.parseInt(getConfigValue("submitter_role_id"));
        long submitterTermsId = Long.parseLong(getConfigValue("submitter_terms_id"));
        long reviewerTermsId = Long.parseLong(getConfigValue("reviewer_terms_id"));

        // create ProjectRoleTermsOfUse default associations
        ProjectTermsOfUseDao projectRoleTermsOfUse = TermsOfUseUtil.getProjectTermsOfUseDao();
        projectRoleTermsOfUse.createProjectRoleTermsOfUse(new Long(projectId).intValue(),
                submitterRoleId, submitterTermsId, DEFAULT_TERMS_SORT_ORDER, 0);

        if (projectTypeId == ProjectType.ID_DEVELOPMENT) {
            int accuracyReviewerRoleId = Integer.parseInt(getConfigValue("accuracy_reviewer_role_id"));
            int failureReviewerRoleId = Integer.parseInt(getConfigValue("failure_reviewer_role_id"));
            int stressReviewerRoleId = Integer.parseInt(getConfigValue("stress_reviewer_role_id"));

            // if it's a development project there are several reviewer roles
            projectRoleTermsOfUse.createProjectRoleTermsOfUse(new Long(projectId).intValue(),
                    accuracyReviewerRoleId, reviewerTermsId, DEFAULT_TERMS_SORT_ORDER, 0);

            projectRoleTermsOfUse.createProjectRoleTermsOfUse(new Long(projectId).intValue(),
                    failureReviewerRoleId, reviewerTermsId, DEFAULT_TERMS_SORT_ORDER, 0);

            projectRoleTermsOfUse.createProjectRoleTermsOfUse(new Long(projectId).intValue(),
                    stressReviewerRoleId, reviewerTermsId, DEFAULT_TERMS_SORT_ORDER, 0);
        } else {
            int reviewerRoleId = Integer.parseInt(getConfigValue("reviewer_role_id"));

            // if it's not development there is a single reviewer role
            projectRoleTermsOfUse.createProjectRoleTermsOfUse(new Long(projectId).intValue(),
                    reviewerRoleId, reviewerTermsId, DEFAULT_TERMS_SORT_ORDER, 0);
        }

        // also add terms for the rest of the reviewer roles
        int primaryScreenerRoleId = Integer.parseInt(getConfigValue("primary_screener_role_id"));
        int aggregatorRoleId = Integer.parseInt(getConfigValue("aggregator_role_id"));
        int finalReviewerRoleId = Integer.parseInt(getConfigValue("final_reviewer_role_id"));

        projectRoleTermsOfUse.createProjectRoleTermsOfUse(new Long(projectId).intValue(),
                primaryScreenerRoleId, reviewerTermsId, DEFAULT_TERMS_SORT_ORDER, 0);
        projectRoleTermsOfUse.createProjectRoleTermsOfUse(new Long(projectId).intValue(),
                aggregatorRoleId, reviewerTermsId, DEFAULT_TERMS_SORT_ORDER, 0);
        projectRoleTermsOfUse.createProjectRoleTermsOfUse(new Long(projectId).intValue(),
                finalReviewerRoleId, reviewerTermsId, DEFAULT_TERMS_SORT_ORDER, 0);
    }


    public void updateVersionDatesInfo(VersionDateInfo versionDateInfo)
            throws CatalogException {

        if (versionDateInfo == null) {
            throw new CatalogException(
                    "Null specified for version info");
        }

        if (ComponentVersionInfo.SPECIFICATION == versionDateInfo.getPhaseId() ||
                ComponentVersionInfo.DEVELOPMENT == versionDateInfo.getPhaseId() ||
                ComponentVersionInfo.COMPLETED == versionDateInfo.getPhaseId()) {
            if (versionDateInfo.getPostingDate() == null ||
                    versionDateInfo.getInitialSubmissionDate() == null ||
                    versionDateInfo.getFinalSubmissionDate() == null ||
                    versionDateInfo.getWinnerAnnouncedDate() == null ||
                    versionDateInfo.getPrice() == 0) {
                throw new CatalogException("Postingdate, initial submissiondate, " +
                        "final submissiondate, winner announced date, and price is required");
            }
            if (versionDateInfo.getPhaseId() == ComponentVersionInfo.SPECIFICATION &&
                    versionDateInfo.getEstimatedDevDate() == null) {
                throw new CatalogException("estimated dev date is required for design phase");
            }

            LocalDDECompVersionDates versionDatesBean;
            Timestamp estimatedDevDate = null;
            Timestamp screeningDate = null;
            Timestamp phaseCompleteDate = null;
            Timestamp aggregationCompleteDate = null;
            Timestamp reviewCompleteDate = null;
            Timestamp productionDate = null;
            if (versionDateInfo.getEstimatedDevDate() != null) {
                estimatedDevDate = new Timestamp(versionDateInfo.getEstimatedDevDate().getTime());
            }
            if (versionDateInfo.getScreeningCompleteDate() != null) {
                screeningDate = new Timestamp(versionDateInfo.getScreeningCompleteDate().getTime());
            }
            if (versionDateInfo.getPhaseCompleteDate() != null) {
                phaseCompleteDate = new Timestamp(versionDateInfo.getPhaseCompleteDate().getTime());
            }
            if (versionDateInfo.getAggregationCompleteDate() != null) {
                aggregationCompleteDate = new Timestamp(versionDateInfo.getAggregationCompleteDate().getTime());
            }
            if (versionDateInfo.getReviewCompleteDate() != null) {
                reviewCompleteDate = new Timestamp(versionDateInfo.getReviewCompleteDate().getTime());
            }
            if (versionDateInfo.getProductionDate() != null) {
                productionDate = new Timestamp(versionDateInfo.getProductionDate().getTime());
            }
            try {
                versionDatesBean =
                        versionDatesHome.findByComponentVersionId(versionDateInfo.getComponentVersionId(), versionDateInfo.getPhaseId());
            } catch (FinderException exception) {

                try {

                    versionDatesHome.create(versionDateInfo.getComponentVersionId(),
                            versionDateInfo.getPhaseId(),
                            new Timestamp(versionDateInfo.getPostingDate().getTime()),
                            new Timestamp(versionDateInfo.getInitialSubmissionDate().getTime()),
                            new Timestamp(versionDateInfo.getFinalSubmissionDate().getTime()),
                            new Timestamp(versionDateInfo.getWinnerAnnouncedDate().getTime()),
                            estimatedDevDate,
                            0.0, versionDateInfo.getStatusId(), versionDateInfo.getLevelId(),
//                            versionDateInfo.getPrice(), versionDateInfo.getStatusId(), versionDateInfo.getLevelId(),
                            screeningDate, phaseCompleteDate, aggregationCompleteDate, reviewCompleteDate,
                            versionDateInfo.getPhaseCompleteDateComment(),
                            versionDateInfo.getAggregationCompleteDateComment(),
                            versionDateInfo.getReviewCompleteDateComment(),
                            versionDateInfo.getScreeningCompleteDateComment(),
                            versionDateInfo.getInitialSubmissionDateComment(),
                            versionDateInfo.getFinalSubmissionDateComment(),
                            versionDateInfo.getWinnerAnnouncedDateComment(), productionDate,
                            versionDateInfo.getProductionDateComment());

                    versionDatesHistoryHome.create(versionDateInfo.getComponentVersionId(),
                            versionDateInfo.getPhaseId(),
                            new Timestamp(versionDateInfo.getPostingDate().getTime()),
                            new Timestamp(versionDateInfo.getInitialSubmissionDate().getTime()),
                            new Timestamp(versionDateInfo.getFinalSubmissionDate().getTime()),
                            new Timestamp(versionDateInfo.getWinnerAnnouncedDate().getTime()),
                            estimatedDevDate,
                            0.0, versionDateInfo.getStatusId(), versionDateInfo.getLevelId(),
//                            versionDateInfo.getPrice(), versionDateInfo.getStatusId(), versionDateInfo.getLevelId(),
                            screeningDate, phaseCompleteDate, aggregationCompleteDate, reviewCompleteDate,
                            versionDateInfo.getPhaseCompleteDateComment(),
                            versionDateInfo.getAggregationCompleteDateComment(),
                            versionDateInfo.getReviewCompleteDateComment(),
                            versionDateInfo.getScreeningCompleteDateComment(),
                            versionDateInfo.getInitialSubmissionDateComment(),
                            versionDateInfo.getFinalSubmissionDateComment(),
                            versionDateInfo.getWinnerAnnouncedDateComment(), productionDate,
                            versionDateInfo.getProductionDateComment());
                    return;
                } catch (CreateException ce) {
                    throw new CatalogException(ce.getMessage());
                }
            }

            if (versionDateInfo.getPhaseId() != versionDatesBean.getPhaseId() || versionDateInfo.getId() == -1L) {

                try {
                    versionDatesHome.create(versionDatesBean.getComponentVersionId(),
                            versionDateInfo.getPhaseId(),
                            new Timestamp(versionDateInfo.getPostingDate().getTime()),
                            new Timestamp(versionDateInfo.getInitialSubmissionDate().getTime()),
                            new Timestamp(versionDateInfo.getFinalSubmissionDate().getTime()),
                            new Timestamp(versionDateInfo.getWinnerAnnouncedDate().getTime()),
                            estimatedDevDate,
//                            versionDateInfo.getPrice(), versionDateInfo.getStatusId(), versionDateInfo.getLevelId(),
                            0.0, versionDateInfo.getStatusId(), versionDateInfo.getLevelId(),
                            screeningDate, phaseCompleteDate, aggregationCompleteDate, reviewCompleteDate,
                            versionDateInfo.getPhaseCompleteDateComment(),
                            versionDateInfo.getAggregationCompleteDateComment(),
                            versionDateInfo.getReviewCompleteDateComment(),
                            versionDateInfo.getScreeningCompleteDateComment(),
                            versionDateInfo.getInitialSubmissionDateComment(),
                            versionDateInfo.getFinalSubmissionDateComment(),
                            versionDateInfo.getWinnerAnnouncedDateComment(), productionDate,
                            versionDateInfo.getProductionDateComment());

                    versionDatesHistoryHome.create(versionDateInfo.getComponentVersionId(),
                            versionDateInfo.getPhaseId(),
                            new Timestamp(versionDateInfo.getPostingDate().getTime()),
                            new Timestamp(versionDateInfo.getInitialSubmissionDate().getTime()),
                            new Timestamp(versionDateInfo.getFinalSubmissionDate().getTime()),
                            new Timestamp(versionDateInfo.getWinnerAnnouncedDate().getTime()),
                            estimatedDevDate,
                            0.0, versionDateInfo.getStatusId(), versionDateInfo.getLevelId(),
//                            versionDateInfo.getPrice(), versionDateInfo.getStatusId(), versionDateInfo.getLevelId(),
                            screeningDate, phaseCompleteDate, aggregationCompleteDate, reviewCompleteDate,
                            versionDateInfo.getPhaseCompleteDateComment(),
                            versionDateInfo.getAggregationCompleteDateComment(),
                            versionDateInfo.getReviewCompleteDateComment(),
                            versionDateInfo.getScreeningCompleteDateComment(),
                            versionDateInfo.getInitialSubmissionDateComment(),
                            versionDateInfo.getFinalSubmissionDateComment(),
                            versionDateInfo.getWinnerAnnouncedDateComment(), productionDate,
                            versionDateInfo.getProductionDateComment());

                    return;
                } catch (CreateException ce) {
                    throw new CatalogException(ce.getMessage());
                }

            } else {

                versionDatesBean.setPostingDate(
                        new Timestamp(versionDateInfo.getPostingDate().getTime()));
                versionDatesBean.setInitialSubmissionDate(
                        new Timestamp(versionDateInfo.getInitialSubmissionDate().getTime()));
                versionDatesBean.setFinalSubmissionDate(
                        new Timestamp(versionDateInfo.getFinalSubmissionDate().getTime()));
                versionDatesBean.setWinnerAnnouncedDate(
                        new Timestamp(versionDateInfo.getWinnerAnnouncedDate().getTime()));
                versionDatesBean.setEstimatedDevDate(estimatedDevDate);
                versionDatesBean.setPrice(0.0);
//                versionDatesBean.setPrice(versionDateInfo.getPrice());
                versionDatesBean.setStatusId(versionDateInfo.getStatusId());
                log.debug("level id" + versionDateInfo.getLevelId());
                versionDatesBean.setLevelId(versionDateInfo.getLevelId());


                versionDatesBean.setScreeningCompleteDate(screeningDate);
                versionDatesBean.setPhaseCompleteDate(phaseCompleteDate);
                log.debug("aggregationCompleteDate: " + aggregationCompleteDate);
                versionDatesBean.setAggregationCompleteDate(aggregationCompleteDate);
                versionDatesBean.setReviewCompleteDate(reviewCompleteDate);
                versionDatesBean.setProductionDate(productionDate);


                versionDatesBean.setAggregationCompleteDateComment(versionDateInfo.getAggregationCompleteDateComment());
                versionDatesBean.setPhaseCompleteDateComment(versionDateInfo.getPhaseCompleteDateComment());
                versionDatesBean.setReviewCompleteDateComment(versionDateInfo.getReviewCompleteDateComment());
                versionDatesBean.setWinnerAnnouncedDateComment(versionDateInfo.getWinnerAnnouncedDateComment());
                versionDatesBean.setInitialSubmissionDateComment(versionDateInfo.getInitialSubmissionDateComment());
                versionDatesBean.setScreeningCompleteDateComment(versionDateInfo.getScreeningCompleteDateComment());
                versionDatesBean.setFinalSubmissionDateComment(versionDateInfo.getFinalSubmissionDateComment());
                versionDatesBean.setProductionDateComment(versionDateInfo.getProductionDateComment());

                try {
                    versionDatesHistoryHome.create(versionDateInfo.getComponentVersionId(),
                            versionDateInfo.getPhaseId(),
                            new Timestamp(versionDateInfo.getPostingDate().getTime()),
                            new Timestamp(versionDateInfo.getInitialSubmissionDate().getTime()),
                            new Timestamp(versionDateInfo.getFinalSubmissionDate().getTime()),
                            new Timestamp(versionDateInfo.getWinnerAnnouncedDate().getTime()),
                            estimatedDevDate,
                            0.0, versionDateInfo.getStatusId(), versionDateInfo.getLevelId(),
//                            versionDateInfo.getPrice(), versionDateInfo.getStatusId(), versionDateInfo.getLevelId(),
                            screeningDate, phaseCompleteDate, aggregationCompleteDate, reviewCompleteDate,
                            versionDateInfo.getPhaseCompleteDateComment(),
                            versionDateInfo.getAggregationCompleteDateComment(),
                            versionDateInfo.getReviewCompleteDateComment(),
                            versionDateInfo.getScreeningCompleteDateComment(),
                            versionDateInfo.getInitialSubmissionDateComment(),
                            versionDateInfo.getFinalSubmissionDateComment(),
                            versionDateInfo.getWinnerAnnouncedDateComment(), productionDate,
                            versionDateInfo.getProductionDateComment());


                } catch (CreateException ce) {
                    throw new CatalogException(ce.getMessage());
                }

            }
        }
    }


    public Collection getCategories() throws CatalogException {
        List containers = new ArrayList();
        Iterator catIterator;
        try {
            catIterator =
                    compcatsHome.findByComponentId(componentId).iterator();
        } catch (FinderException impossible) {
            throw new CatalogException(impossible.toString());
        }
        while (catIterator.hasNext()) {
            LocalDDECategories category = ((LocalDDECompCategories)
                    catIterator.next()).getCategories();
            containers.add(new Category(
                    ((Long) category.getPrimaryKey()).longValue(),
                    category.getName(), category.getDescription(), null));
        }
        Collections.sort(containers, new Comparators.CategorySorter());
        return containers;
    }

    public Collection getTechnologies() throws CatalogException {
        List technologies = new ArrayList();
        Iterator techIterator;
        try {
            techIterator = comptechHome.findByCompVersId(versionId).iterator();
        } catch (FinderException impossible) {
            throw new CatalogException(impossible.toString());
        }
        while (techIterator.hasNext()) {
            LocalDDETechnologyTypes technology = ((LocalDDECompTechnology)
                    techIterator.next()).getTechnologyTypes();
            technologies.add(new Technology(
                    ((Long) technology.getPrimaryKey()).longValue(),
                    technology.getName(), technology.getDescription()));
        }
        Collections.sort(technologies, new Comparators.TechnologySorter());
        return technologies;
    }

    public Collection getTeamMemberRoles() throws CatalogException {
        List memberRoles = new ArrayList();
        /* the user_role table does not exist
        Iterator roleIterator;
        try {
            roleIterator = userroleHome.findByCompVersId(versionId).iterator();
        } catch (FinderException impossible) {
            throw new CatalogException(impossible.toString());
        }
        while (roleIterator.hasNext()) {
            LocalDDEUserRole role = (LocalDDEUserRole) roleIterator.next();
            long userId =
                    ((Long) role.getUserMaster().getPrimaryKey()).longValue();
            String username;
            try {
                username =
                        principalmgrHome.create().getUser(userId).getName();
            } catch (RemoteException exception) {
                throw new EJBException(exception.toString());
            } catch (Exception exception) {
                throw new CatalogException(exception.toString());
            }
            memberRoles.add(new TeamMemberRole(
                    ((Long) role.getPrimaryKey()).longValue(),
                    userId, username, ((Long) role.getRoles().getPrimaryKey()).longValue(),
                    role.getRoles().getName(), role.getRoles().getDescription(),
                    role.getDescription()));
        }
        Collections.sort(memberRoles, new Comparators.TeamMemberRoleSorter());*/
        return memberRoles;
    }

    public Collection getDocuments() throws CatalogException {
        List docs = new ArrayList();
        Iterator docIterator;
        try {
            docIterator = docHome.findByCompVersId(versionId).iterator();
        } catch (FinderException impossible) {
            throw new CatalogException(impossible.toString());
        }
        while (docIterator.hasNext()) {
            LocalDDECompDocumentation doc =
                    (LocalDDECompDocumentation) docIterator.next();
            docs.add(new Document(
                    ((Long) doc.getPrimaryKey()).longValue(),
                    doc.getDocumentName(), doc.getUrl(),
                    doc.getDocumentTypeId()));
        }

        //Add aggregation scorecard documents
        /*
        if (isAggregated(1)) {
            docs.add(new Document("Aggregate Design Scorecard",
                    "/review/publicaggregation.do?id=" + getProjectId(1),
                    Document.OTHER));
        }
        if (isAggregated(2)) {
            docs.add(new Document("Aggregate Development Scorecard",
                    "/review/publicaggregation.do?id=" + getProjectId(2),
                    Document.OTHER));
        }*/

        Collections.sort(docs, new Comparators.DocumentSorter());
        return docs;
    }

    public Document getDocument(long documentId) throws CatalogException {
        try {
            LocalDDECompDocumentation doc = docHome.findByPrimaryKey(new Long(documentId));
            return new Document(
                    ((Long) doc.getPrimaryKey()).longValue(),
                    doc.getDocumentName(), doc.getUrl(),
                    doc.getDocumentTypeId());
        } catch (FinderException impossible) {
            throw new CatalogException(impossible.toString());
        }
    }

    public Collection getDownloads() throws CatalogException {
        List downloads = new ArrayList();
        Iterator downIterator;
        try {
            downIterator = downloadHome.findByCompVersId(versionId).iterator();
        } catch (FinderException impossible) {
            throw new CatalogException(impossible.toString());
        }
        while (downIterator.hasNext()) {
            LocalDDECompDownload download =
                    (LocalDDECompDownload) downIterator.next();
            downloads.add(new Download(
                    ((Long) download.getPrimaryKey()).longValue(),
                    download.getDescription(), download.getUrl()));
        }
        Collections.sort(downloads, new Comparators.DownloadSorter());
        return downloads;
    }

    public Download getDownload(long downloadId) throws CatalogException {
        try {
            LocalDDECompDownload download = downloadHome.findByPrimaryKey(new Long(downloadId));
            return new Download(
                    ((Long) download.getPrimaryKey()).longValue(),
                    download.getDescription(), download.getUrl());
        } catch (FinderException impossible) {
            throw new CatalogException(impossible.toString());
        }
    }

    public Collection getExamples() throws CatalogException {
        List examples = new ArrayList();
        Iterator exIterator;
        try {
            exIterator = exampleHome.findByCompVersId(versionId).iterator();
        } catch (FinderException impossible) {
            throw new CatalogException(impossible.toString());
        }
        while (exIterator.hasNext()) {
            LocalDDECompExamples example =
                    (LocalDDECompExamples) exIterator.next();
            examples.add(new Example(
                    ((Long) example.getPrimaryKey()).longValue(),
                    example.getDescription(), example.getUrl()));
        }
        Collections.sort(examples, new Comparators.ExampleSorter());
        return examples;
    }

    public Collection getDependencies() throws CatalogException {
        List summaries = new ArrayList();
        Iterator depIterator;
        try {
            depIterator = depHome.findByCompVersId(versionId).iterator();
        } catch (FinderException impossible) {
            throw new CatalogException(impossible.toString());
        }
        while (depIterator.hasNext()) {
            LocalDDECompDependencies dependency =
                    (LocalDDECompDependencies) depIterator.next();
            LocalDDECompVersions dependee = dependency.getChildCompVersions();
            summaries.add(CatalogBean.generateSummary(
                    dependee.getCompCatalog(), dependee));
        }
        Collections.sort(summaries, new Comparators.ComponentSummarySorter());
        return summaries;
    }

    public Collection getReviews() throws CatalogException {
        List reviews = new ArrayList();
        Iterator reviewIterator;
        try {
            reviewIterator = reviewHome.findByCompVersId(versionId).iterator();
        } catch (FinderException impossible) {
            throw new CatalogException(impossible.toString());
        }
        while (reviewIterator.hasNext()) {
            LocalDDECompReviews review =
                    (LocalDDECompReviews) reviewIterator.next();
            long userId =
                    ((Long) review.getUserMaster().getPrimaryKey()).longValue();
            String username;
            try {
                username =
                        principalmgrHome.create().getUser(userId).getName();
            } catch (RemoteException exception) {
                throw new EJBException(exception.toString());
            } catch (Exception exception) {
                throw new CatalogException(exception.toString());
            }
            reviews.add(new Review(
                    ((Long) review.getPrimaryKey()).longValue(),
                    userId, username,
                    new Date(review.getReviewTime().getTime()),
                    review.getRating(), review.getComments()));
        }
        Collections.sort(reviews, new Comparators.ReviewSorter());
        return reviews;
    }

    public ForumCategory getForumCategory() throws CatalogException {
        Forums forumsBean = getForumsBean();
        ComponentVersionInfo info = getVersionInfo();
        Collection categories = new HashSet();
        Iterator categoryIterator;
        try {
            categoryIterator = compforumHome.findByCompVersId(versionId).iterator();
        } catch (FinderException impossible) {
            throw new CatalogException(impossible.toString());
        }
        while (categoryIterator.hasNext()) {
            long categoryId = ((LocalDDECompForumXref)
                    categoryIterator.next()).getCategoryId();
            try {
                ArrayList data = forumsBean.getSoftwareForumCategoryData(categoryId);
                Date startDate = (Date) data.get(0);
                long status = Long.parseLong((String) data.get(1));
                categories.add(new ForumCategory(categoryId, startDate, status, version, info.getVersionLabel()));
            } catch (ForumCategoryNotFoundException fe) {
                throw new CatalogException("Forum category not found: " + fe.toString());
            } catch (RemoteException exception) {
                throw new EJBException(exception.toString());
            }
        }

        if (categories.size() > 1) {
            throw new CatalogException(
                    "The number of categories is greater than one");
        } else if (categories.size() == 0) {
            return null;
        }
        return (ForumCategory) categories.iterator().next();
    }

    public ForumCategory getActiveForumCategory() throws CatalogException {
        Forums forumsBean = getForumsBean();
        Collection categories = new HashSet();
        Iterator versionIterator = getAllVersionInfo().iterator();
        while (versionIterator.hasNext()) {
            ComponentVersionInfo info =
                    (ComponentVersionInfo) versionIterator.next();
            Iterator categoryIterator;
            try {
                categoryIterator = compforumHome.findByCompVersId(info.getVersionId()).iterator();
            } catch (FinderException impossible) {
                throw new CatalogException(impossible.toString());
            }
            while (categoryIterator.hasNext()) {
                long categoryId = ((LocalDDECompForumXref)
                        categoryIterator.next()).getCategoryId();
                try {
                    ArrayList data = forumsBean.getSoftwareForumCategoryData(categoryId);
                    Date startDate = (Date) data.get(0);
                    long status = Long.parseLong((String) data.get(1));
                    ForumCategory forumCategory = new ForumCategory(categoryId, startDate, status, info.getVersion(), info.getVersionLabel());
                    if (forumCategory.getStatus() == ForumCategory.ACTIVE) {
                        categories.add(forumCategory);
                    }
                } catch (ForumCategoryNotFoundException fe) {
                    throw new CatalogException("Forum category not found: " + fe.toString());
                } catch (RemoteException exception) {
                    throw new EJBException(exception.toString());
                }
            }
        }
        if (categories.size() > 1) {
            throw new CatalogException(
                    "The number of categories is not exactly one");
        } else if (categories.size() == 0) {
            return null;
        }
        return (ForumCategory) categories.iterator().next();
    }

    public Collection getClosedForumCategories() throws CatalogException {
        Forums forumsBean = getForumsBean();
        List categories = new ArrayList();
        Iterator versionIterator = getAllVersionInfo().iterator();
        while (versionIterator.hasNext()) {
            ComponentVersionInfo info =
                    (ComponentVersionInfo) versionIterator.next();
            Iterator categoryIterator;
            try {
                categoryIterator = compforumHome.findByCompVersId(info.getVersionId()).iterator();
            } catch (FinderException impossible) {
                throw new CatalogException(impossible.toString());
            }
            while (categoryIterator.hasNext()) {
                long categoryId = ((LocalDDECompForumXref)
                        categoryIterator.next()).getCategoryId();
                try {
                    ArrayList data = forumsBean.getSoftwareForumCategoryData(categoryId);
                    Date startDate = (Date) data.get(0);
                    long status = Long.parseLong((String) data.get(1));
                    ForumCategory forumCategory = new ForumCategory(categoryId, startDate, status, info.getVersion(), info.getVersionLabel());
                    if (forumCategory.getStatus() == ForumCategory.CLOSED) {
                        categories.add(forumCategory);
                    }
                } catch (ForumCategoryNotFoundException fe) {
                    throw new CatalogException("Forum category not found: " + fe.toString());
                } catch (RemoteException exception) {
                    throw new EJBException(exception.toString());
                }
            }
        }
        Collections.sort(categories, new Comparators.ForumCategorySorter());
        return categories;
    }

    public void addCategory(long categoryId) throws CatalogException {
        LocalDDECompCatalog component;
        try {
            component = catalogHome.findByPrimaryKey(new Long(componentId));
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }

        LocalDDECategories category;
        try {
            category = categoriesHome.findByPrimaryKey(new Long(categoryId));
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }

        boolean alreadyExists = true;
        try {
            compcatsHome.findByComponentIdAndCategoryId(componentId,
                    categoryId);
        } catch (ObjectNotFoundException exception) {
            alreadyExists = false;
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }

        if (!alreadyExists) {
            try {
                compcatsHome.create(component, category);
            } catch (CreateException exception) {
                ejbContext.setRollbackOnly();
                throw new CatalogException(exception.toString());
            }
        }
    }

    public void addTechnology(long technologyId) throws CatalogException {
        LocalDDECompVersions versionBean;
        try {
            versionBean = versionsHome.findByPrimaryKey(new Long(versionId));
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }

        LocalDDETechnologyTypes technology;
        try {
            technology =
                    technologiesHome.findByPrimaryKey(new Long(technologyId));
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }

        boolean alreadyExists = true;
        try {
            comptechHome.findByCompVersIdAndTechnologyId(versionId,
                    technologyId);
        } catch (ObjectNotFoundException exception) {
            alreadyExists = false;
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }

        if (!alreadyExists) {
            try {
                comptechHome.create(versionBean, technology);
            } catch (CreateException exception) {
                ejbContext.setRollbackOnly();
                throw new CatalogException(exception.toString());
            }
        }
    }

    public TeamMemberRole addTeamMemberRole(TeamMemberRole role)
            throws CatalogException {
        return null;
        /*
        if (role == null) {
            throw new CatalogException(
                    "Null specified for team member role");
        }
        if (role.getId() != -1) {
            throw new CatalogException(
                    "Specified role already exists for this version");
        }

        LocalDDECompVersions versionBean;
        try {
            versionBean = versionsHome.findByPrimaryKey(new Long(versionId));
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }

        LocalDDEUserMaster user;
        String username;
        try {
            user = userHome.findByPrimaryKey(new Long(role.getUserId()));
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }
        try {
            username = principalmgrHome.create().getUser(role.getUserId()).
                    getName();
        } catch (RemoteException exception) {
            throw new EJBException(exception.toString());
        } catch (Exception exception) {
            throw new CatalogException(exception.toString());
        }

        LocalDDERoles roleBean;
        try {
            roleBean = rolesHome.findByPrimaryKey(new Long(role.getRoleId()));
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }

        try {
            LocalDDEUserRole userRole = userroleHome.create(
                    role.getDescription(), user, versionBean, roleBean);
            return new TeamMemberRole(
                    ((Long) userRole.getPrimaryKey()).longValue(),
                    role.getUserId(), username, role.getRoleId(),
                    roleBean.getName(), roleBean.getDescription(),
                    userRole.getDescription());
        } catch (CreateException exception) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(exception.toString());
        }*/
    }

    public Document addDocument(Document document) throws CatalogException {
        if (document == null) {
            throw new CatalogException("Null specified for document");
        }
        if (document.getId() != -1) {
            throw new CatalogException(
                    "Specified document already exists");
        }

        LocalDDECompVersions versionBean;
        try {
            versionBean = versionsHome.findByPrimaryKey(new Long(versionId));
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }

        try {
            LocalDDECompDocumentation doc = docHome.create(
                    document.getType(), document.getName(),
                    document.getURL().toString(), versionBean);
            return new Document(
                    ((Long) doc.getPrimaryKey()).longValue(), document.getName(),
                    document.getURL(), document.getType());
        } catch (CreateException exception) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(exception.toString());
        }
    }

    public Example addExample(Example example) throws CatalogException {
        if (example == null) {
            throw new CatalogException("Null specified for example");
        }
        if (example.getId() != -1) {
            throw new CatalogException(
                    "Specified example already exists");
        }

        LocalDDECompVersions versionBean;
        try {
            versionBean = versionsHome.findByPrimaryKey(new Long(versionId));
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }

        try {
            LocalDDECompExamples exampleBean = exampleHome.create(
                    example.getURL().toString(),
                    example.getDescription(), versionBean);
            return new Example(
                    ((Long) exampleBean.getPrimaryKey()).longValue(),
                    example.getDescription(), example.getURL());
        } catch (CreateException exception) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(exception.toString());
        }
    }

    public Download addDownload(Download download) throws CatalogException {
        if (download == null) {
            throw new CatalogException("Null specified for download");
        }
        if (download.getId() != -1) {
            throw new CatalogException(
                    "Specified download already exists");
        }

        LocalDDECompVersions versionBean;
        try {
            versionBean = versionsHome.findByPrimaryKey(new Long(versionId));
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }

        try {
            LocalDDECompDownload downloadBean = downloadHome.create(
                    download.getURL().toString(),
                    download.getDescription(), versionBean);
            return new Download(
                    ((Long) downloadBean.getPrimaryKey()).longValue(),
                    download.getDescription(), download.getURL());
        } catch (CreateException exception) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(exception.toString());
        }
    }

    public void addDependency(long dependeeVersionId) throws CatalogException {
        LocalDDECompVersions versionBean;
        try {
            versionBean = versionsHome.findByPrimaryKey(new Long(versionId));
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }

        LocalDDECompVersions dependee;
        try {
            dependee =
                    versionsHome.findByPrimaryKey(new Long(dependeeVersionId));
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }

        boolean alreadyExists = true;
        try {
            depHome.findByCompVersIdAndChildId(versionId, dependeeVersionId);
        } catch (ObjectNotFoundException exception) {
            alreadyExists = false;
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }

        if (!alreadyExists) {
            try {
                depHome.create(versionBean, dependee);
            } catch (CreateException exception) {
                ejbContext.setRollbackOnly();
                throw new CatalogException(exception.toString());
            }
        }
    }

    public Review addReview(Review review) throws CatalogException {
        if (review == null) {
            throw new CatalogException("Null specified for review");
        }
        if (review.getId() != -1) {
            throw new CatalogException(
                    "specified review already exists");
        }

        LocalDDECompVersions versionBean;
        try {
            versionBean = versionsHome.findByPrimaryKey(new Long(versionId));
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }

        LocalDDEUserMaster user;
        String username;
        try {
            user = userHome.findByPrimaryKey(new Long(review.getUserId()));
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }
        try {
            username = principalmgrHome.create().getUser(review.getUserId()).
                    getName();
        } catch (RemoteException exception) {
            throw new EJBException(exception.toString());
        } catch (Exception exception) {
            throw new CatalogException(exception.toString());
        }

        try {
            LocalDDECompReviews reviewBean = reviewHome.create(
                    new Timestamp((new Date()).getTime()),
                    review.getRating(), review.getComments(), versionBean, user);
            return new Review(
                    ((Long) reviewBean.getPrimaryKey()).longValue(),
                    review.getUserId(), username,
                    new Date(reviewBean.getReviewTime().getTime()),
                    review.getRating(), review.getComments());
        } catch (CreateException exception) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(exception.toString());
        }
    }

    public void updateDocument(Document document) throws CatalogException {
        if (document == null) {
            throw new CatalogException("Null specified for document");
        }
        if (document.getId() == -1) {
            throw new CatalogException(
                    "Specified document does not exist in the catalog");
        }

        LocalDDECompDocumentation docBean;
        try {
            docBean = docHome.findByPrimaryKey(new Long(document.getId()));
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }
        docBean.setDocumentName(document.getName());
        docBean.setUrl(document.getURL().toString());
        docBean.setDocumentTypeId(document.getType());
    }

    public void updateExample(Example example) throws CatalogException {
        if (example == null) {
            throw new CatalogException("Null specified for example");
        }
        if (example.getId() == -1) {
            throw new CatalogException(
                    "Specified example does not exist in the catalog");
        }

        LocalDDECompExamples exampleBean;
        try {
            exampleBean =
                    exampleHome.findByPrimaryKey(new Long(example.getId()));
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }
        exampleBean.setDescription(example.getDescription());
        exampleBean.setUrl(example.getURL().toString());
    }

    public void updateDownload(Download download) throws CatalogException {
        if (download == null) {
            throw new CatalogException("Null specified for download");
        }
        if (download.getId() == -1) {
            throw new CatalogException(
                    "Specified download does not exist in the catalog");
        }

        LocalDDECompDownload downloadBean;
        try {
            downloadBean =
                    downloadHome.findByPrimaryKey(new Long(download.getId()));
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }
        downloadBean.setDescription(download.getDescription());
        downloadBean.setUrl(download.getURL().toString());
    }

    public void updateReview(Review review) throws CatalogException {
        if (review == null) {
            throw new CatalogException("Null specified for review");
        }
        if (review.getId() == -1) {
            throw new CatalogException(
                    "specified review does not exist in the catalog");
        }

        LocalDDECompReviews reviewBean;
        try {
            reviewBean = reviewHome.findByPrimaryKey(new Long(review.getId()));
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }
        reviewBean.setRating(review.getRating());
        reviewBean.setComments(review.getComments());
        reviewBean.setReviewTime(new Timestamp((new Date()).getTime()));
    }

    public void removeCategory(long categoryId) throws CatalogException {
        try {
            compcatsHome.findByComponentIdAndCategoryId(componentId,
                    categoryId).remove();
        } catch (ObjectNotFoundException exception) {
            throw new CatalogException(
                    "Specified category is not associated with this component: "
                            + exception.toString());
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        } catch (RemoveException exception) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(exception.toString());
        }
    }

    public void removeTechnology(long technologyId) throws CatalogException {
        try {
            comptechHome.findByCompVersIdAndTechnologyId(versionId,
                    technologyId).remove();
        } catch (ObjectNotFoundException exception) {
            throw new CatalogException(
                    "Specified technology is not associated with this component: "
                            + exception.toString());
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        } catch (RemoveException exception) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(exception.toString());
        }
    }

    public void removeTeamMemberRole(long memberRoleId)
            throws CatalogException {
        /* user_role does not exist
        try {
            userroleHome.findByPrimaryKey(new Long(memberRoleId)).remove();
        } catch (ObjectNotFoundException exception) {
            throw new CatalogException(
                    "Specified team member role does not exist in the catalog: "
                    + exception.toString());
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        } catch (RemoveException exception) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(exception.toString());
        }*/
    }

    public void removeDependency(long dependeeId) throws CatalogException {
        try {
            depHome.findByCompVersIdAndChildId(versionId, dependeeId).remove();
        } catch (ObjectNotFoundException exception) {
            throw new CatalogException("This component version is not dependent on specified component version: "
                    + exception.toString());
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        } catch (RemoveException exception) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(exception.toString());
        }
    }

    public String removeDocument(long documentId) throws CatalogException {
        String urlOfFile = null;
        try {
            LocalDDECompDocumentation compDoc = docHome.findByPrimaryKey(new Long(documentId));
            urlOfFile = compDoc.getUrl();
            compDoc.remove();
        } catch (ObjectNotFoundException exception) {
            throw new CatalogException(
                    "Specified document does not exist in the catalog: "
                            + exception.toString());
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        } catch (RemoveException exception) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(exception.toString());
        }
        return urlOfFile;
    }

    public void removeExample(long exampleId) throws CatalogException {
        try {
            exampleHome.findByPrimaryKey(new Long(exampleId)).remove();
        } catch (ObjectNotFoundException exception) {
            throw new CatalogException(
                    "Specified example does not exist in the catalog: "
                            + exception.toString());
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        } catch (RemoveException exception) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(exception.toString());
        }
    }

    public void removeDownload(long downloadId) throws CatalogException {
        try {
            downloadHome.findByPrimaryKey(new Long(downloadId)).remove();
        } catch (ObjectNotFoundException exception) {
            throw new CatalogException(
                    "Specified download location does not exist in the catalog: "
                            + exception.toString());
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        } catch (RemoveException exception) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(exception.toString());
        }
    }

    public void removeReview(long reviewId) throws CatalogException {
        try {
            reviewHome.findByPrimaryKey(new Long(reviewId)).remove();
        } catch (ObjectNotFoundException exception) {
            throw new CatalogException(
                    "Specified review does not exist in the catalog: "
                            + exception.toString());
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        } catch (RemoveException exception) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(exception.toString());
        }
    }

    /**
     * This method will apply the restrictions on the component download operation.
     * It will follow the next process:
     * - check for special unlimited download permission
     * - check download permission
     * - grant access if rated
     * - grant access if registered to a current dev, design, assembly, testing, architecture or studio project
     * - deny access if the user has already downloaded more than N components
     * - deny access if the requested component doesn't is not "public"
     *
     * @param subject the security subject of the requester
     * @return true if permission is granted.
     * @throws CatalogException
     */
    public boolean canDownload(TCSubject subject) throws CatalogException {
        if (subject == null) {
            throw new CatalogException("Null specified for subject");
        }
        log.debug("canDownload called (user, component): " + subject.getUserId() + ", " + componentId);

        int maxPublicDownloads = getMaxPublicDownloads();
        int maxDaysFromRegistration = getMaxDaysFromRegistration();

        try {
            // check for special permissions
            // first general
            PolicyRemote checker = policyHome.create();
            UnlimitedDownloadPermission udp = new UnlimitedDownloadPermission();
            log.debug("checking for permission..." + udp.getName() + "-");
            boolean hasSpecialPermission = checker.checkPermission(subject, udp);
            log.debug("hasGeneralUnlimitedPermission: " + hasSpecialPermission);
            if (hasSpecialPermission) {
                return true;
            }

            // then for this particular component
            udp = new UnlimitedDownloadPermission(componentId);
            hasSpecialPermission = checker.checkPermission(subject, udp);
            log.debug("checking for permission..." + udp.getName() + "-");
            log.debug("hasComponentUnlimitedPermission: " + hasSpecialPermission);
            if (hasSpecialPermission) {
                return true;
            }

            // check for regular permission
            boolean hasPermission = checker.checkPermission(subject, new DownloadPermission(componentId));
            log.debug("hasPermission: " + hasPermission);
            if (!hasPermission) {
                return false;
            }

            // check for rating
            UserServices us = UserServicesLocator.getService();
            boolean isRated = us.isRated(subject.getUserId());
            log.debug("us.isRated(subject.getUserId()): " + isRated);
            if (isRated) {
                return true;
            }

            // check if the user has registrations
            boolean hasCompetitionRegistration = us.hasCompetitionRegistration(subject.getUserId(), maxDaysFromRegistration, competitionCategories);
            log.debug("us.hasRegistration(subject.getUserId(), competitionCategories): " + hasCompetitionRegistration);
            if (hasCompetitionRegistration) {
                return true;
            }

            // check how many downloads the user has already done
            int numberDownloads = trackingHome.numberComponentDownloadsByUserId(subject.getUserId());
            log.debug("numberDownloads: " + numberDownloads);
            if (numberDownloads >= maxPublicDownloads) {
                throw new ReachedQuotaException("You have exceeded the number of allowed downloads");
            }

            // check if the component is for public download
            LocalDDECompCatalog comp = catalogHome.findByPrimaryKey(new Long(componentId));
            if (comp.getPublicInd() != 1) {
                log.debug("Not public component.");
                throw new NonPublicComponentException("You are not allowed to download non-public components");
            }
            log.debug("Public component.");
        } catch (ObjectNotFoundException e) {
            throw new EJBException(e);
        } catch (FinderException e) {
            throw new EJBException(e);
        } catch (NamingException e) {
            throw new EJBException(e);
        } catch (GeneralSecurityException exception) {
            throw new CatalogException(exception.toString());
        } catch (CreateException exception) {
            throw new CatalogException(exception.toString());
        } catch (RemoteException e) {
            throw new EJBException(e);
        }

        return true;
    }

    /**
     * Gets the maximum public downloads allowed
     *
     * @return the maximum public downloads allowed
     * @throws NumberFormatException
     * @throws ConfigManagerException
     */
    public int getMaxPublicDownloads() {
        int maxPublicDownloads;
        try {
            maxPublicDownloads = Integer.parseInt(getConfigValue("max_public_downloads"));
        } catch (NumberFormatException e) {
            throw new EJBException(e);
        } catch (ConfigManagerException e) {
            throw new EJBException(e);
        }
        return maxPublicDownloads;
    }

    /**
     * Gets the maximum days from registration to count as "currently" registered
     *
     * @return the maximum days from registration to count as "currently" registered
     * @throws NumberFormatException
     * @throws ConfigManagerException
     */
    public int getMaxDaysFromRegistration() {
        int maxDaysFromRegistration;
        try {
            maxDaysFromRegistration = Integer.parseInt(getConfigValue("max_days_from_registration"));
        } catch (NumberFormatException e) {
            throw new EJBException(e);
        } catch (ConfigManagerException e) {
            throw new EJBException(e);
        }
        return maxDaysFromRegistration;
    }


    /**
     * Gets the number of distinct components downloaded by the user
     *
     * @param userId the user id to query
     * @return the number of distinct components downloaded by the user
     * @throws FinderException
     */
    public int getNumberComponentsDownloaded(long userId) {
        int numComponentsDownloaded;
            try {
                numComponentsDownloaded = trackingHome.numberComponentDownloadsByUserId(userId);
            } catch (FinderException e) {
                throw new EJBException(e);
            }
        return numComponentsDownloaded;
    }

    public void trackDownload(long userId, long downloadId, long licenseId)
            throws CatalogException {
        LocalDDECompVersions versionBean;
        try {
            versionBean = versionsHome.findByPrimaryKey(new Long(versionId));
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }

        LocalDDEUserMaster userBean;
        try {
            userBean = userHome.findByPrimaryKey(new Long(userId));
        } catch (ObjectNotFoundException exception) {
            throw new CatalogException(
                    "Specified user does not exist: " + exception.toString());
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }

        LocalDDELicenseLevel licenseBean;
        try {
            licenseBean = licenseHome.findByPrimaryKey(new Long(licenseId));
        } catch (ObjectNotFoundException exception) {
            throw new CatalogException(
                    "Specified license does not exist in the catalog: "
                            + exception.toString());
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }

        LocalDDECompDownload downloadBean;
        try {
            downloadBean = downloadHome.findByPrimaryKey(new Long(downloadId));
        } catch (ObjectNotFoundException exception) {
            throw new CatalogException(
                    "Specified download does not exist in the catalog: "
                            + exception.toString());
        } catch (FinderException exception) {
            throw new CatalogException(exception.toString());
        }

        Timestamp currentTime = new Timestamp((new Date()).getTime());
        double price = getVersionInfo().getPrice();
        LicenseLevel costCalculator;
        try {
            costCalculator = new LicenseLevel(
                    ((Long) licenseBean.getPrimaryKey()).longValue(),
                    licenseBean.getDescription(),
                    licenseBean.getPriceMultiplier(),
                    Double.parseDouble(getConfigValue("price_per_unit")));
        } catch (ConfigManagerException exception) {
            throw new CatalogException(
                    "Failed to obtain configuration data: " + exception.toString());
        }
        try {
            trackingHome.create(price, currentTime, versionBean, userBean,
                    licenseBean, downloadBean,
                    costCalculator.calculateUnitCost(price));
        } catch (CreateException exception) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(exception.toString());
        }
    }

    private String getConfigValue(String name)
            throws ConfigManagerException {
        ConfigManager config = ConfigManager.getInstance();
        if (config.existsNamespace(CONFIG_NAMESPACE)) {
            config.refresh(CONFIG_NAMESPACE);
        } else {
            config.add(CONFIG_NAMESPACE,
                    ConfigManager.CONFIG_PROPERTIES_FORMAT);
        }
        return config.getString(CONFIG_NAMESPACE, name);
    }

    public String getNamespace() {
        return CONFIG_NAMESPACE;
    }

    public Enumeration getConfigPropNames() {
        Vector propNames = new Vector();
        propNames.add("price_per_unit");
        propNames.add("requestor_role_id");
        propNames.add("collab_forum_template");
        propNames.add("spec_forum_template");
        propNames.add("administrator_role");
        propNames.add("user_role");
        return propNames.elements();
    }


    public void updateDates()
            throws RemoteException, CatalogException {

        Context con = null;
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = new InitialContext();
            DataSource ds = (DataSource) con.lookup("java:InformixDS");
            conn = ds.getConnection();
            String query = "SELECT cv.component_id, " +
                    "       cv.version " +
                    "  FROM comp_version_dates cvd, " +
                    "       comp_versions cv" +
                    " WHERE initial_submission_date = EXTEND(CURRENT - 1 UNITS DAY, YEAR TO DAY)" +
                    "   AND cv.comp_vers_id = cvd.comp_vers_id" +
                    "   AND NOT EXISTS (SELECT s.submitter_id" +
                    "              FROM submitters s, submissions sb" +
                    "             WHERE s.comp_version_id = cvd.comp_vers_id" +
                    "               AND sb.submitter_id = s.submitter_id" +
                    "               AND sb.date >= cvd.posting_date " +
                    "               AND s.phase_id = cvd.phase_id)";
            ps = conn.prepareStatement(query);
            rs = ps.executeQuery();

            ComponentManagerHome componentManagerHome = (ComponentManagerHome)
                    PortableRemoteObject.narrow(con.lookup("ComponentManagerEJB"),
                            ComponentManagerHome.class);
            ComponentManager componentManager = null;
            while (rs.next()) {

                componentManager = componentManagerHome.create(rs.getLong("component_id"), rs.getLong("version"));
                ComponentVersionInfo ver = componentManager.getVersionInfo();
                VersionDateInfo verDateInfo = componentManager.getVersionDateInfo(ver.getVersionId(), ver.getPhase());
// millis in a second *seconds in a minute * minutes in an hour * hours in a day * 7 days
                verDateInfo.setInitialSubmissionDate(new Date(verDateInfo.getInitialSubmissionDate().getTime() + 1000 * 60 * 60 * 24 * 7));

                verDateInfo.setScreeningCompleteDate(new Date(verDateInfo.getScreeningCompleteDate().getTime() + 1000 * 60 * 60 * 24 * 7));

                verDateInfo.setReviewCompleteDate(new Date(verDateInfo.getReviewCompleteDate().getTime() + 1000 * 60 * 60 * 24 * 7));

                verDateInfo.setAggregationCompleteDate(new Date(verDateInfo.getAggregationCompleteDate().getTime() + 1000 * 60 * 60 * 24 * 7));

                if (verDateInfo.getWinnerAnnouncedDate() != null)
                    verDateInfo.setWinnerAnnouncedDate(new Date(verDateInfo.getWinnerAnnouncedDate().getTime() + 1000 * 60 * 60 * 24 * 7));

                if (verDateInfo.getFinalSubmissionDate() != null)
                    verDateInfo.setFinalSubmissionDate(new Date(verDateInfo.getFinalSubmissionDate().getTime() + 1000 * 60 * 60 * 24 * 7));

                if (verDateInfo.getEstimatedDevDate() != null)
                    verDateInfo.setEstimatedDevDate(new Date(verDateInfo.getEstimatedDevDate().getTime() + 1000 * 60 * 60 * 24 * 7));

                if (verDateInfo.getEstimatedDevDate() != null)
                    verDateInfo.setPhaseCompleteDate(new Date(verDateInfo.getPhaseCompleteDate().getTime() + 1000 * 60 * 60 * 24 * 7));

                verDateInfo.setStatusId(VersionDateInfo.RE_POSTING);

                componentManager.updateVersionDatesInfo(verDateInfo);

            }


        } catch (Exception e) {
            System.out.println("big error: " + e);
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (Exception ignore) {
                System.out.println("rs   close problem " + ignore);
            }
            try {
                if (ps != null) ps.close();
            } catch (Exception ignore) {
                System.out.println("ps   close problem" + ignore);
            }
            try {
                if (conn != null) conn.close();
            } catch (Exception ignore) {
                System.out.println("conn close problem" + ignore);
            }
            rs = null;
            ps = null;
            conn = null;
        }

    }

    /**
     * Gets the project id for the project of the given type associated with this component version
     *
     * @param projectType design or development
     * @return the project id or -1 if no project was found
     * @throws CatalogException
     */
    public long getProjectId(long projectType) throws CatalogException {
        try {
            ProjectTrackerV2 pt = projectTrackerHome.create();
            return pt.getProjectIdByComponentVersionId(getVersionInfo().getVersionId(), projectType);
        } catch (RemoteException e) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(e.toString());
        } catch (CreateException e) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(e.toString());
        }
    }

    /**
     * Determines whether or not the project of the given type for this component version has yielded a
     * publicly readable aggregation worksheet
     *
     * @param projectType design or development
     * @return true if there is an aggregation worksheet for the given type, false otherwise
     * @throws CatalogException
     */
    public boolean isAggregated(long projectType) throws CatalogException {
        return true;
        /*long projectId = getProjectId(projectType);
        if (projectId < 0) return false;
        try {
            DocumentManager dm = documentManagerHome.create();
            return dm.isProjectAggregated(projectId);
        } catch (RemoteException e) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(e.toString());
        } catch (CreateException e) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(e.toString());
        }*/
    }

    public void ejbActivate() {
    }

    public void ejbPassivate() {
        /*
         * All fields are either home interfaces of other enterprise beans or
         * instances of <code>SessionContext</code>, so nothing needs to be done
         * to enable serialization.
         */
    }

    public void ejbRemove() {
    }

    public void setSessionContext(SessionContext context) {
        ejbContext = context;
    }

    // All your database are belong to us

    /**
     * <p>Generates the description for the notification event correponding to posting to a forum associated with
     * current version of current component. Such a description may be used to populate the
     * 'notification_event.description' column when adding a new notification event.</p>
     *
     * @param type a <code>String</code> describing the type of notification
     * @return a <code>String</code> providing the description of a notification event for current version of current
     *         component.
     * @throws CatalogException
     */
    private String createNotificationEventDescription(String type) throws CatalogException {
        if (type == null) {
            throw new NullPointerException("type should not be null");
        }

        StringBuffer buffer = new StringBuffer();

        ComponentInfo info = getComponentInfo();
        ComponentVersionInfo version = getVersionInfo();

        String category = "";

        try {
            // Locate the base category for the component.
            LocalDDECompCatalog catalog = catalogHome.findByPrimaryKey(new Long(info.getId()));
            LocalDDECategories categories = categoriesHome.findByPrimaryKey(new Long(catalog.getRootCategory()));
            category = categories.getName();
        } catch (FinderException e) {
            throw new CatalogException(e.toString());
        }

        buffer.append(category);
        buffer.append(" ");
        buffer.append(info.getName());
        buffer.append(" ");
        buffer.append(version.getVersionLabel());
        buffer.append(" - " + type);

        return buffer.toString().trim();
    }

    private Forums getForumsBean() throws CatalogException {
        Forums forumsBean = null;
        try {
            Context context = TCContext.getInitial(ApplicationServer.FORUMS_HOST_URL);
            ForumsHome forumsHome = (ForumsHome) context.lookup(ForumsHome.EJB_REF_NAME);
            forumsBean = forumsHome.create();
        } catch (NamingException e) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(
                    "Failed to connect to forums server EJB: "
                            + e.toString());
        } catch (CreateException e) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(e.toString());
        } catch (RemoteException e) {
            ejbContext.setRollbackOnly();
            throw new CatalogException(e.toString());
        }
        return forumsBean;
    }
}
