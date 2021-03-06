package com.topcoder.web.truveo.dao;

import com.topcoder.web.truveo.model.ContestProperty;

import java.util.List;

/**
 * @author dok
 * @version $Revision: 70119 $ Date: 2005/01/01 00:00:00
 *          Create Date: Jul 28, 2006
 */
public interface ContestPropertyDAO {
    ContestProperty find(Integer id);

    List getProperties();
}
