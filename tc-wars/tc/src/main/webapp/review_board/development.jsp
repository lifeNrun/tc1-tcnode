<%@ page import="com.topcoder.shared.dataAccess.resultSet.ResultSetContainer"%>
<%@  page language="java"  %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<% ResultSetContainer memberList = (ResultSetContainer)request.getAttribute("memberList");%>
<%@ taglib uri="rsc-taglib.tld" prefix="rsc" %>
<%@ taglib uri="tc.tld" prefix="tc" %>
<%@ taglib uri="tc-webtags.tld" prefix="tc-webtag" %>

<html>

<head>

<title>Development Review Board at TopCoder</title>

<jsp:include page="../script.jsp" />
<jsp:include page="/style.jsp">
  <jsp:param name="key" value="tc_stats"/>
</jsp:include>

</head>

<body>

<jsp:include page="../top.jsp" />

<table width="100%" border="0" cellpadding="0" cellspacing="0">
    <tr valign="top">

<!-- Left Column Begins -->
        <td width="180">
         <jsp:include page="/includes/global_left.jsp">
            <jsp:param name="node" value="dev_review_board"/>
         </jsp:include>
        </td>
<!-- Left Column Ends -->

<!-- Gutter Begins -->
        <td width="10"><img src="/i/clear.gif" width="10" height="1"></td>
<!-- Gutter Ends -->

<!-- Center Column Begins -->
<td class="bodyText" WIDTH="100%">

<jsp:include page="/page_title.jsp">
    <jsp:param name="image" value="comp_development"/>
    <jsp:param name="title" value="Meet the Development Review Board"/>
</jsp:include>

            <table border="0" cellspacing="0" cellpadding="0" width="100%">
                <tr>
                    <td class="bodyText"><img src="/i/clear.gif" width="240" height="1" border="0"><br>
                        <%-- TCS Release 2.2.0 (TCS-55) : The request passes project type ID instead of component
                             phase type ID when referrring to review board type --%>
<%--
                        <p align="right" ><a href="/tc?module=ReviewBoard&ph=112" class="bodyText"><strong>Go to Design Review Board</strong></a></p>
--%>
                        <p align="right" ><a href="/tc?module=ReviewBoard&pt=1" class="bodyText"><strong>Go to Design Review Board</strong></a></p>

                        <p>TopCoder Software utilizes past component development winners and accomplished TopCoder competitors to staff the Development Review Board.
                        These reviewers verify that each development submission meets the required functionality, coding style, adheres to the component design
                        and contains a suitable test suite.  Furthermore, to certify the component belongs in the TopCoder catalog, the development review board
                        adds a suite of test cases including stress, accuracy and failure tests to the component.</p>

                        <p>The following members have agreed to participate on the TopCoder Development Review Board:</p>
                    </td>
                </tr>
            </table>

            <hr width="100%" size="1" noshade="noshade" />

            <table width="100%" border="0" cellpadding="5" cellspacing="0">
                <tr valign="top">
                    <% int i=0; %>
                    <rsc:iterator list="<%=memberList%>" id="resultRow">
                      <% if (i<3) { %>
                        <td>
                     <% } else {  %>
                        <tr valign="top">
                     <% } %>
                        <% i++; %>
                        <div align="center">
                        <a href="/tc?module=MemberProfile&amp;cr=<rsc:item row="<%=resultRow%>" name="user_id"/>"><img src="<rsc:item row="<%=resultRow%>" name="image_path" ifNull="/i/m/nophoto.jpg"/>" alt="<rsc:item row="<%=resultRow%>" name="handle"/>" width="126" height="140" border="0"  class="myStatsPhoto"/></a>
                        <br/>
                        <span class="bodyText"><tc-webtag:handle coderId='<%=resultRow.getIntItem("user_id")%>' context="development"/></span>

                        </div>
                     <% if (i<3) { %>
                          </td>
                     <% } else {  %>
                        </td>
                        </tr>
                        <% i=0; %>
                     <% } %>
                    </rsc:iterator>
                </tr>
            </table>


            <p align="middle" ><A href="mailto:support@topcoder.com" class="bodyText">Have a question about the TopCoder Development Review Board?</strong></a></p>

            <p><br></p>
         </td>
<!-- Center Column Ends -->

<!-- Gutter -->
      <td width="10"><img src="/i/clear.gif" width="10" height="1" border="0"></td>
<!-- Gutter Ends -->

<!-- Right Column Begins -->
       <td width="170"><img src="/i/clear.gif" width="170" height="1" border="0"><br/>
            <jsp:include page="../public_right.jsp">
                <jsp:param name="level1" value="review_board"/>
                <jsp:param name="level2" value="competition"/>
            </jsp:include>
        </td>
<!-- Right Column Ends -->

<!-- Gutter -->
      <td width="10"><img src="/i/clear.gif" width="10" height="1" border="0"></td>
<!-- Gutter Ends -->
    </tr>
</table>

<jsp:include page="../foot.jsp" />

</body>

</html>
