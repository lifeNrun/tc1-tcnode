package com.topcoder.web.common.dao;

import com.topcoder.web.common.model.CoderType;

import java.util.List;

/**
 * @author dok
 * @version $Revision: 53709 $ Date: 2005/01/01 00:00:00
 *          Create Date: May 11, 2006
 */
public interface CoderTypeDAO {
    List getCoderTypes();

    CoderType find(Integer id);
}
