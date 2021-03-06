package com.topcoder.web.privatelabel.controller.request.googlechina05;

import com.topcoder.web.privatelabel.controller.request.BaseCredentialReminder;
import com.topcoder.web.privatelabel.Constants;
import com.topcoder.shared.util.TCResourceBundle;

/**
 * @author dok
 * @version $Revision: 43007 $ $Date$
 *          Create Date: Oct 31, 2005
 */
public class CredentialReminder extends BaseCredentialReminder {

    protected String getStartPage() {
        return getBundle().getProperty("google_china_05_credentials_page");
    }

    protected String getSuccessPage() {
        return getBundle().getProperty("google_china_05_credentials_sent_page");
    }
}


