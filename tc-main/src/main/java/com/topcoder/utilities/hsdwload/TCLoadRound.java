package com.topcoder.utilities.hsdwload;

/**
 * TCLoadRound.java
 *
 * TCLoadRound loads round information from the transactional database and
 * populates tables in the data warehouse.
 *
 * The tables that are built by this load procedure are:
 *
 * <ul>
 * <li>rating (additional information is populated in the rating table)</li>
 * <li>problem_submission</li>
 * <li>system_test_case</li>
 * <li>system_test_result</li>
 * <li>contest</li>
 * <li>problem</li>
 * <li>round</li>
 * <li>room</li>
 * <li>room_result</li>
 * <li>coder_problem</li>
 * </ul>
 *
 * @author Christopher Hopkins [TCid: darkstalker] (chrism_hopkins@yahoo.com)
 * @version $Revision: 31164 $
 */

import com.topcoder.shared.util.DBMS;
import com.topcoder.shared.util.logging.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

public class TCLoadRound extends TCLoad {
    private static Logger log = Logger.getLogger(TCLoadRound.class);
    protected java.sql.Timestamp fStartTime = null;
    protected java.sql.Timestamp fLastLogTime = null;

    // The following set of variables are all configureable from the command
    // line by specifying -variable (where the variable is after the //)
    // followed by the new value
    private int fRoundId = -1;                 // roundid
    private int STATUS_FAILED = 0;    // failed
    private int STATUS_SUCCEEDED = 1;    // succeeded
    private int CODING_SEGMENT_ID = 2;    // codingseg
    private int STATUS_OPENED = 120;  // opened
    private int STATUS_PASSED_SYS_TEST = 150;  // passsystest
    private int STATUS_FAILED_SYS_TEST = 160;  // failsystest
    private int CONTEST_ROOM = 2;    // contestroom
    private int ROUND_LOG_TYPE = 1;    // roundlogtype
    private int CHALLENGE_NULLIFIED = 92;   // challengenullified
    private int STUDENT_GROUP_ID = 1800001;
    private boolean FULL_LOAD = false;//fullload

    /**
     * This Hashtable stores the start date of a particular round so
     * that we don't have to look it up each time.
     */
    private Hashtable fRoundStartHT = new Hashtable();

    /**
     * Constructor. Set our usage message here.
     */
    public TCLoadRound() {
        DEBUG = false;

        USAGE_MESSAGE = new String(
                "TCLoadRound parameters - defaults in ():\n" +
                "  -roundid number       : Round ID to load\n" +
                "  [-failed number]      : Failed status for succeeded column    (0)\n" +
                "  [-succeeded number]   : Succeeded status for succeeded column (1)\n" +
                "  [-codingseg number]   : ID for beginning of coding segment    (2)\n" +
                "  [-opened number]      : Problem_status of opened              (120)\n" +
                "  [-passsystest number] : Problem_status of passed system test  (150)\n" +
                "  [-failsystest number] : Problem_status of failed system test  (160)\n" +
                "  [-contestroom number] : Type id for contest rooms             (2)\n" +
                "  [-roundlogtype number] : Log type id for this load            (1)\n" +
                "  [-challengenullified number] : id for nullified challenges    (92)\n" +
                "  [-studentgroup number] : id for the student group             (1800001)\n" +
                "  [-fullload boolean] : true-clean round load, false-selective  (false)\n");
    }

    /**
     * This method is passed any parameters passed to this load
     */
    public boolean setParameters(Hashtable params) {
        try {
            Integer tmp;
            Boolean tmpBool;
            fRoundId = retrieveIntParam("roundid", params, false, true).intValue();

            tmp = retrieveIntParam("failed", params, true, true);
            if (tmp != null) {
                STATUS_FAILED = tmp.intValue();
                log.info("New failed is " + STATUS_FAILED);
            }

            tmp = retrieveIntParam("succeeded", params, true, true);
            if (tmp != null) {
                STATUS_SUCCEEDED = tmp.intValue();
                log.info("New succeeded is " + STATUS_SUCCEEDED);
            }

            tmp = retrieveIntParam("codingseg", params, true, true);
            if (tmp != null) {
                CODING_SEGMENT_ID = tmp.intValue();
                log.info("New coding segment id is " + CODING_SEGMENT_ID);
            }

            tmp = retrieveIntParam("opened", params, true, true);
            if (tmp != null) {
                STATUS_OPENED = tmp.intValue();
                log.info("New opened is " + STATUS_OPENED);
            }

            tmp = retrieveIntParam("passsystest", params, true, true);
            if (tmp != null) {
                STATUS_PASSED_SYS_TEST = tmp.intValue();
                log.info("New passsystest is " + STATUS_PASSED_SYS_TEST);
            }

            tmp = retrieveIntParam("failsystest", params, true, true);
            if (tmp != null) {
                STATUS_FAILED_SYS_TEST = tmp.intValue();
                log.info("New failsystest  is " + STATUS_FAILED_SYS_TEST);
            }

            tmp = retrieveIntParam("contestroom", params, true, true);
            if (tmp != null) {
                CONTEST_ROOM = tmp.intValue();
                log.info("New contestroom id is " + CONTEST_ROOM);
            }

            tmp = retrieveIntParam("roundlogtype", params, true, true);
            if (tmp != null) {
                ROUND_LOG_TYPE = tmp.intValue();
                log.info("New roundlogtype is " + ROUND_LOG_TYPE);
            }

            tmp = retrieveIntParam("challengenullified", params, true, true);
            if (tmp != null) {
                CHALLENGE_NULLIFIED = tmp.intValue();
                log.info("New challengenullified id is " + CHALLENGE_NULLIFIED);
            }

            tmp = retrieveIntParam("studentgroup", params, true, true);
            if (tmp != null) {
                STUDENT_GROUP_ID = tmp.intValue();
                log.info("New studentgroup id is " + STUDENT_GROUP_ID);
            }

            tmpBool = retrieveBooleanParam("fullload", params, true);
            if (tmpBool != null) {
                FULL_LOAD = tmpBool.booleanValue();
                log.info("New fullload flag is " + FULL_LOAD);
            }


        } catch (Exception ex) {
            setReasonFailed(ex.getMessage());
            return false;
        }

        return true;
    }

    /**
     * This method performs the load for the round information tables
     */
    public boolean performLoad() {
        try {
            log.info("Loading round: " + fRoundId);

            fStartTime = new java.sql.Timestamp(System.currentTimeMillis());

            getLastUpdateTime();

            clearRound();

            loadContest();

            loadRound();

            loadProblem();

            loadProblemSubmission();

            loadSystemTestCase();

            loadSystemTestResult();

            loadRoom();

            loadRoomResult();

            loadRating();

            loadCoderProblem();

            loadChallenge();

            setLastUpdateTime();

            log.info("SUCCESS: Round " + fRoundId +
                    " load ran successfully.");
            return true;
        } catch (Exception ex) {
            setReasonFailed(ex.getMessage());
            return false;
        }
    }

    private void clearRound() throws Exception {
        PreparedStatement ps = null;
        ArrayList a = null;

        try {
            a = new ArrayList();


            if (FULL_LOAD) {
                a.add(new String("DELETE FROM coder_level"));
                a.add(new String("DELETE FROM coder_division"));
                a.add(new String("DELETE FROM room_result WHERE round_id = ?"));
                a.add(new String("DELETE FROM round_division"));
                a.add(new String("DELETE FROM coder_problem_summary"));
                a.add(new String("DELETE FROM system_test_case WHERE problem_id in (SELECT problem_id FROM round_problem WHERE round_id = ?)"));
                a.add(new String("DELETE FROM round_problem"));
                a.add(new String("DELETE FROM challenge WHERE round_id = ?"));
                a.add(new String("DELETE FROM coder_problem WHERE round_id = ?"));
                a.add(new String("DELETE FROM room WHERE round_id = ?"));
                a.add(new String("DELETE FROM system_test_result WHERE round_id = ?"));
                a.add(new String("DELETE FROM problem_submission WHERE round_id = ?"));
                a.add(new String("DELETE FROM problem WHERE round_id = ?"));
                a.add(new String("UPDATE rating SET first_rated_round_id = null WHERE first_rated_round_id = ?"));
                a.add(new String("UPDATE rating SET last_rated_round_id = null WHERE last_rated_round_id = ?"));

            } else {
                a.add(new String("DELETE FROM coder_level WHERE coder_id IN (SELECT coder_id FROM room_result WHERE attended = 'Y' AND round_id = ?)"));
                a.add(new String("DELETE FROM coder_division WHERE coder_id IN (SELECT coder_id FROM room_result WHERE attended = 'Y' AND round_id = ?)"));
                a.add(new String("DELETE FROM room_result WHERE round_id = ?"));
                a.add(new String("DELETE FROM round_division WHERE round_id = ?"));
                a.add(new String("DELETE FROM coder_problem_summary WHERE coder_id IN (SELECT coder_id FROM room_result WHERE attended = 'Y' AND round_id = ?)"));
                a.add(new String("DELETE FROM system_test_case WHERE problem_id in (SELECT problem_id FROM round_problem WHERE round_id = ?)"));
                a.add(new String("DELETE FROM round_problem WHERE round_id = ?"));
                a.add(new String("DELETE FROM challenge WHERE round_id = ?"));
                a.add(new String("DELETE FROM coder_problem WHERE round_id = ?"));
                a.add(new String("DELETE FROM system_test_result WHERE round_id = ?"));
                a.add(new String("DELETE FROM problem_submission WHERE round_id = ?"));
                a.add(new String("DELETE FROM problem WHERE round_id = ?"));
                a.add(new String("UPDATE rating SET first_rated_round_id = null WHERE first_rated_round_id = ?"));
                a.add(new String("UPDATE rating SET last_rated_round_id = null WHERE last_rated_round_id = ?"));

            }

            int count = 0;
            for (int i = 0; i < a.size(); i++) {
                ps = prepareStatement((String) a.get(i), TARGET_DB);
                if (((String) a.get(i)).indexOf('?') > -1)
                    ps.setInt(1, fRoundId);
                count = ps.executeUpdate();
                log.info("" + count + " rows: " + (String) a.get(i));
            }
        } catch (SQLException sqle) {
            DBMS.printSqlException(true, sqle);
            throw new Exception("clearing data failed.\n" +
                    sqle.getMessage());
        } finally {
            close(ps);
        }
    }


    private void getLastUpdateTime() throws Exception {
        Statement stmt = null;
        ResultSet rs = null;
        StringBuffer query = null;

        try {
            query = new StringBuffer(100);
            query.append("select timestamp from update_log where log_id = ");
            query.append("(select max(log_id) from update_log where log_type_id = " + ROUND_LOG_TYPE + ")");
            stmt = createStatement(TARGET_DB);
            rs = stmt.executeQuery(query.toString());
            if (rs.next()) {
                fLastLogTime = rs.getTimestamp(1);
            } else {
                throw new SQLException("Last log time not found in update_log table");
            }
        } catch (SQLException sqle) {
            DBMS.printSqlException(true, sqle);
            throw new Exception("Failed to retrieve last log time.\n" +
                    sqle.getMessage());
        } finally {
            close(rs);
            close(stmt);
        }
    }

    /**
     * Here we want to load any new rating information into the rating table.
     */
    private void loadRating() throws Exception {
        int count = 0;
        int retVal = 0;
        PreparedStatement psSel = null;
        PreparedStatement psSelNumCompetitions = null;
        PreparedStatement psSelRatedRounds = null;
        PreparedStatement psSelMinMaxRatings = null;
        PreparedStatement psUpd = null;
        ResultSet rs = null;
        ResultSet rs2 = null;
        StringBuffer query = null;

        try {
            // Get all the coders that participated in this round
            query = new StringBuffer(100);
            query.append("SELECT rr.coder_id ");    // 1
            query.append("  FROM room_result rr ");
            query.append(" WHERE rr.round_id = ? ");
            query.append("   AND rr.attended = 'Y' ");
            query.append("   AND EXISTS ");
            query.append("     (SELECT 'dok' ");
            query.append("        FROM user_group_xref ugx");
            query.append("       WHERE ugx.login_id = rr.coder_id");
            query.append("         AND ugx.group_id = " + STUDENT_GROUP_ID + ")");

            psSel = prepareStatement(query.toString(), SOURCE_DB);

            query = new StringBuffer(100);
            query.append("SELECT min(rr.new_rating) ");  // 1
            query.append("       ,max(rr.new_rating) "); // 2
            query.append("  FROM room_result rr ");
            query.append(" WHERE rr.coder_id = ? ");
            query.append("   AND rr.attended = 'Y' ");
            query.append("   AND rr.new_rating > 0 ");
            query.append("   AND EXISTS ");
            query.append("     (SELECT 'dok' ");
            query.append("        FROM user_group_xref ugx");
            query.append("       WHERE ugx.login_id = rr.coder_id");
            query.append("         AND ugx.group_id = " + STUDENT_GROUP_ID + ")");

            psSelMinMaxRatings = prepareStatement(query.toString(), SOURCE_DB);

            // No need to filter admins here as they have already been filtered from
            // the DW rating table
            query = new StringBuffer(100);
            query.append("SELECT first_rated_round_id "); // 1
            query.append("       ,last_rated_round_id "); // 2
            query.append("  FROM rating ");
            query.append(" WHERE coder_id = ?");
            psSelRatedRounds = prepareStatement(query.toString(), TARGET_DB);

            // No need to filter admins here as they have already been filtered from
            // the DW rating table
            query = new StringBuffer(100);
            query.append("SELECT count(*) ");     // 1
            query.append("  FROM room_result rr ");
            query.append(" WHERE rr.attended = 'Y' ");
            query.append("   AND rr.coder_id = ? ");
            psSelNumCompetitions = prepareStatement(query.toString(), TARGET_DB);

            query = new StringBuffer(100);
            query.append("UPDATE rating ");
            query.append("   SET first_rated_round_id = ? ");  // 1
            query.append("       ,last_rated_round_id = ? ");  // 2
            query.append("       ,lowest_rating = ? ");        // 3
            query.append("       ,highest_rating = ? ");       // 4
            query.append("       ,num_competitions = ? ");     // 5
            query.append(" WHERE coder_id = ?");               // 6
            psUpd = prepareStatement(query.toString(), TARGET_DB);

            psSel.setInt(1, fRoundId);
            rs = psSel.executeQuery();

            while (rs.next()) {
                int coder_id = rs.getInt(1);

                int num_competitions = -1;
                int first_rated_round_id = -1;
                int last_rated_round_id = -1;
                int lowest_rating = -1;
                int highest_rating = -1;

                // Get the existing first and last rated round ids in case they are
                // already there.
                psSelRatedRounds.clearParameters();
                psSelRatedRounds.setInt(1, coder_id);
                rs2 = psSelRatedRounds.executeQuery();
                if (rs2.next()) {
                    if (rs2.getString(1) != null)
                        first_rated_round_id = rs2.getInt(1);
                    if (rs2.getString(2) != null)
                        last_rated_round_id = rs2.getInt(2);
                }

                close(rs2);

                // Get the number of competitions
                psSelNumCompetitions.clearParameters();
                psSelNumCompetitions.setInt(1, coder_id);
                rs2 = psSelNumCompetitions.executeQuery();
                if (rs2.next()) {
                    num_competitions = rs2.getInt(1);
                }

                close(rs2);

                // Get the new min/max ratings to see if we
                psSelMinMaxRatings.clearParameters();
                psSelMinMaxRatings.setInt(1, coder_id);
                rs2 = psSelMinMaxRatings.executeQuery();
                if (rs2.next()) {
                    lowest_rating = rs2.getInt(1);
                    highest_rating = rs2.getInt(2);
                }

                close(rs2);

                // Check to see if any of the round ids need to be updated to be this
                // round id. If the round we are loading is prior to the first rated
                // round (or it isn't set) we set this round as the first rated round.
                // If the round we are loading is greater than the last rated round
                // (or it isn't set), we set this round as the last rated round
                if (first_rated_round_id == -1 ||
                        getRoundStart(fRoundId).compareTo(getRoundStart(first_rated_round_id)) < 0)
                    first_rated_round_id = fRoundId;

                if (last_rated_round_id == -1 ||
                        getRoundStart(fRoundId).compareTo(getRoundStart(last_rated_round_id)) > 0)
                    last_rated_round_id = fRoundId;

                // Finally, do update
                psUpd.clearParameters();
                psUpd.setInt(1, first_rated_round_id);  // first_rated_round_id
                psUpd.setInt(2, last_rated_round_id);   // last_rated_round_id
                psUpd.setInt(3, lowest_rating);         // lowest_rating
                psUpd.setInt(4, highest_rating);        // highest_rating
                psUpd.setInt(5, num_competitions);      // num_competitions
                psUpd.setInt(6, coder_id);              // coder_id

                retVal = psUpd.executeUpdate();
                count = count + retVal;
                if (retVal != 1) {
                    throw new SQLException("TCLoadCoders: Insert for coder_id " +
                            coder_id +
                            " modified " + retVal + " rows, not one.");
                }

                printLoadProgress(count, "rating");
            }

            log.info("Rating records updated = " + count);
        } catch (SQLException sqle) {
            DBMS.printSqlException(true, sqle);
            throw new Exception("Load of 'rating' table failed.\n" +
                    sqle.getMessage());
        } finally {
            close(rs);
            close(rs2);
            close(psSel);
            close(psSelNumCompetitions);
            close(psSelRatedRounds);
            close(psSelMinMaxRatings);
            close(psUpd);
        }
    }

    /**
     * This method loads the 'problem_submission' table which holds
     * information for a given round and given coder, the results of a
     * particular problem
     */
    private void loadProblemSubmission() throws Exception {
        int retVal = 0;
        int count = 0;
        PreparedStatement psSel = null;
        PreparedStatement psIns = null;
        PreparedStatement psDel = null;
        ResultSet rs = null;
        StringBuffer query = null;

        try {
            query = new StringBuffer(100);
            query.append(" SELECT cs.round_id");              //1
            query.append(" ,cs.coder_id ");            //2
            query.append(" , (SELECT cm.problem_id FROM component cm WHERE cm.component_id = cs.component_id)");          //3
            query.append(" ,cs.points ");              //4
            query.append(" ,cs.status_id ");           //5
            query.append(" ,cs.language_id ");         //6
            query.append(" ,s.open_time ");            //7
            query.append(" ,cs.submission_number ");   //8
            query.append(" ,s.submission_text ");      //9
            query.append(" ,s.submit_time ");          //10
            query.append(" ,s.submission_points ");    //11
            query.append("  ,(SELECT status_desc ");   //12
            query.append(" FROM problem_status_lu ");
            query.append(" WHERE problem_status_id = cs.status_id) ");
            query.append(" ,c.compilation_text");      //13
            query.append(" ,s.submission_number");     //14
            query.append(" FROM component_state cs ");
            query.append(" LEFT OUTER JOIN submission s ");
            query.append(" ON cs.component_state_id = s.component_state_id");
            query.append(" LEFT OUTER JOIN compilation c ");
            query.append(" ON cs.component_state_id = c.component_state_id");
            query.append(" WHERE cs.round_id = ?");
            query.append("   AND EXISTS ");
            query.append("     (SELECT 'dok' ");
            query.append("        FROM user_group_xref ugx");
            query.append("       WHERE ugx.login_id = cs.coder_id");
            query.append("         AND ugx.group_id = " + STUDENT_GROUP_ID + ")");

            psSel = prepareStatement(query.toString(), SOURCE_DB);

            query = new StringBuffer(100);
            query.append("INSERT INTO problem_submission ");
            query.append("      (round_id ");            // 1
            query.append("       ,coder_id ");           // 2
            query.append("       ,problem_id ");         // 3
            query.append("       ,final_points ");       // 4
            query.append("       ,status_id ");          // 5
            query.append("       ,language_id ");        // 6
            query.append("       ,open_time ");          // 7
            query.append("       ,submission_number ");  // 8
            query.append("       ,submission_text ");    // 9
            query.append("       ,submit_time ");        // 10
            query.append("       ,submission_points ");  // 11
            query.append("       ,status_desc ");        // 12
            query.append("       ,last_submission) ");   // 13
            query.append("VALUES (");
            query.append("?,?,?,?,?,?,?,?,?,?,");  // 10
            query.append("?,?,?)");                // 12 total values
            psIns = prepareStatement(query.toString(), TARGET_DB);

            query = new StringBuffer(100);
            query.append("DELETE FROM problem_submission ");
            query.append(" WHERE round_id = ? ");
            query.append("   AND coder_id = ? ");
            query.append("   AND problem_id = ?");
            query.append("   AND submission_number = ?");
            psDel = prepareStatement(query.toString(), TARGET_DB);

            // On to the load
            psSel.setInt(1, fRoundId);
            rs = psSel.executeQuery();

            while (rs.next()) {
                int round_id = rs.getInt(1);
                int coder_id = rs.getInt(2);
                int problem_id = rs.getInt(3);
                int submission_number = rs.getInt(14);
                int last_submission = 0;
                if (rs.getInt(8) > 0) {  //they submitted at least once
                    last_submission = rs.getInt(8) == submission_number ? 1 : 0;
                }

                psDel.clearParameters();
                psDel.setInt(1, round_id);
                psDel.setInt(2, coder_id);
                psDel.setInt(3, problem_id);
                psDel.setInt(4, submission_number);
                psDel.executeUpdate();

                psIns.clearParameters();
                psIns.setInt(1, rs.getInt(1));  // round_id
                psIns.setInt(2, rs.getInt(2));  // coder_id
                psIns.setInt(3, rs.getInt(3));  // problem_id
                psIns.setFloat(4, rs.getFloat(4));  // final_points
                psIns.setInt(5, rs.getInt(5));  // status_id
                psIns.setInt(6, rs.getInt(6));  // language_id
                psIns.setLong(7, rs.getLong(7));  // open_time
                psIns.setInt(8, rs.getInt(14));  // submission_number
                if (Arrays.equals(getBytes(rs, 9), "".getBytes()))
                    setBytes(psIns, 9, getBytes(rs, 13));       // use compilation_text
                else
                    setBytes(psIns, 9, getBytes(rs, 9));       // use submission_text
                psIns.setLong(10, rs.getLong(10));  // submit_time
                psIns.setFloat(11, rs.getFloat(11));  // submission_points
                psIns.setString(12, rs.getString(12));  // status_desc
                psIns.setInt(13, last_submission);  // last_submission

                retVal = psIns.executeUpdate();
                count += retVal;
                if (retVal != 1) {
                    throw new SQLException("TCLoadRound: Insert for coder_id " +
                            coder_id + ", round_id " + round_id +
                            ", problem_id " + problem_id +
                            " modified " + retVal + " rows, not one.");
                }

                printLoadProgress(count, "problem_submission");
            }

            log.info("Problem_submission records copied = " + count);
        } catch (SQLException sqle) {
            DBMS.printSqlException(true, sqle);
            throw new Exception("Load of 'problem_submission' table failed.\n" +
                    sqle.getMessage());
        } finally {
            close(rs);
            close(psSel);
            close(psIns);
            close(psDel);
        }
    }

    /**
     * This load the 'system_test_case' table
     */
    private void loadSystemTestCase() throws Exception {
        int retVal = 0;
        int count = 0;
        PreparedStatement psSel = null;
        PreparedStatement psIns = null;
        PreparedStatement psDel = null;
        ResultSet rs = null;
        StringBuffer query = null;

        try {
            query = new StringBuffer(100);
            query.append("SELECT stc.test_case_id ");      // 1
            query.append("       ,comp.problem_id ");       // 2
            query.append("       ,stc.args ");             // 3
            query.append("       ,stc.expected_result ");  // 4
            query.append("       ,CURRENT ");              // 5
            query.append("  FROM system_test_case stc, component comp ");
            query.append(" WHERE comp.component_id in (SELECT component_id FROM round_component WHERE round_id = ?)");
            query.append(" AND comp.component_id = stc.component_id");
            psSel = prepareStatement(query.toString(), SOURCE_DB);

            query = new StringBuffer(100);
            query.append("INSERT INTO system_test_case ");
            query.append("      (test_case_id ");      // 1
            query.append("       ,problem_id ");       // 2
            query.append("       ,args ");             // 3
            query.append("       ,expected_result ");  // 4
            query.append("       ,modify_date) ");     // 5
            query.append("VALUES ( ");
            query.append("?,?,?,?,?)");  // 5 total values
            psIns = prepareStatement(query.toString(), TARGET_DB);

            query = new StringBuffer(100);
            query.append("DELETE FROM system_test_case ");
            query.append(" WHERE test_case_id = ? ");
            query.append("   AND problem_id = ?");
            psDel = prepareStatement(query.toString(), TARGET_DB);

            // On to the load
            psSel.setInt(1, fRoundId);
            rs = psSel.executeQuery();

            while (rs.next()) {
                int test_case_id = rs.getInt(1);
                int problem_id = rs.getInt(2);

                psDel.clearParameters();
                psDel.setInt(1, test_case_id);
                psDel.setInt(2, problem_id);
                psDel.executeUpdate();

                psIns.clearParameters();
                psIns.setInt(1, rs.getInt(1));  // test_case_id
                psIns.setInt(2, rs.getInt(2));  // problem_id
                setBytes(psIns, 3, getBlobObject(rs, 3));  // args
                setBytes(psIns, 4, getBlobObject(rs, 4));  // expected_result
                psIns.setTimestamp(5, rs.getTimestamp(5));  // modify_date

                retVal = psIns.executeUpdate();
                count += retVal;
                if (retVal != 1) {
                    throw new SQLException("TCLoadRound: Insert for test_case_id " +
                            test_case_id + ", problem_id " + problem_id +
                            " modified more than one row.");
                }

                printLoadProgress(count, "system_test_case");
            }

            log.info("System_test_case records copied = " + count);
        } catch (SQLException sqle) {
            DBMS.printSqlException(true, sqle);
            throw new Exception("Load of 'system_test_case' table failed.\n" +
                    sqle.getMessage());
        } finally {
            close(rs);
            close(psSel);
            close(psIns);
            close(psDel);
        }
    }

    /**
     * This load the 'system_test_result' table which holds the results
     * of the system tests for a give round, coder and problem.
     */
    private void loadSystemTestResult() throws Exception {
        int retVal = 0;
        int count = 0;
        PreparedStatement psSel = null;
        PreparedStatement psIns = null;
        PreparedStatement psDel = null;
        ResultSet rs = null;
        StringBuffer query = null;

        try {
            query = new StringBuffer(100);
            query.append("SELECT str.coder_id ");           // 1
            query.append("       ,str.round_id ");          // 2
            query.append("       ,comp.problem_id ");        // 3
            query.append("       ,str.test_case_id ");      // 4
            query.append("       ,str.num_iterations ");    // 5
            query.append("       ,str.processing_time ");   // 6
            query.append("       ,str.deduction_amount ");  // 7
            query.append("       ,str.timestamp ");         // 8
            query.append("       ,str.viewable ");          // 9
            query.append("       ,str.received ");          // 10
            query.append("       ,str.succeeded ");         // 11
            query.append("       ,str.message ");           // 12
            query.append("  FROM system_test_result str, component comp ");
            query.append(" WHERE str.round_id = ?");
            query.append(" AND comp.component_id = str.component_id");
            query.append("   AND EXISTS ");
            query.append("     (SELECT 'dok' ");
            query.append("        FROM user_group_xref ugx");
            query.append("       WHERE ugx.login_id = str.coder_id");
            query.append("         AND ugx.group_id = " + STUDENT_GROUP_ID + ")");

            psSel = prepareStatement(query.toString(), SOURCE_DB);

            query = new StringBuffer(100);
            query.append("INSERT INTO system_test_result ");
            query.append("      (coder_id ");           // 1
            query.append("       ,round_id ");          // 2
            query.append("       ,problem_id ");        // 3
            query.append("       ,test_case_id ");      // 4
            query.append("       ,num_iterations ");    // 5
            query.append("       ,processing_time ");   // 6
            query.append("       ,deduction_amount ");  // 7
            query.append("       ,timestamp ");         // 8
            query.append("       ,viewable ");          // 9
            query.append("       ,received ");          // 10
            query.append("       ,succeeded ");         // 11
            query.append("       ,message) ");          // 12
            query.append("VALUES (");
            query.append("?,?,?,?,?,?,?,?,?,?,");  // 10 values
            query.append("?,?)");                   // 12 total values
            psIns = prepareStatement(query.toString(), TARGET_DB);

            query = new StringBuffer(100);
            query.append("DELETE FROM system_test_result ");
            query.append(" WHERE coder_id = ? ");
            query.append("   AND round_id = ? ");
            query.append("   AND problem_id = ? ");
            query.append("   AND test_case_id = ?");
            psDel = prepareStatement(query.toString(), TARGET_DB);

            // On to the load
            psSel.setInt(1, fRoundId);
            rs = psSel.executeQuery();

            while (rs.next()) {
                int coder_id = rs.getInt(1);
                int round_id = rs.getInt(2);
                int problem_id = rs.getInt(3);
                int test_case_id = rs.getInt(4);

                psDel.clearParameters();
                psDel.setInt(1, coder_id);
                psDel.setInt(2, round_id);
                psDel.setInt(3, problem_id);
                psDel.setInt(4, test_case_id);
                psDel.executeUpdate();

                psIns.clearParameters();
                psIns.setInt(1, rs.getInt(1));  // coder_id
                psIns.setInt(2, rs.getInt(2));  // round_id
                psIns.setInt(3, rs.getInt(3));  // problem_id
                psIns.setInt(4, rs.getInt(4));  // test_case_id
                psIns.setInt(5, rs.getInt(5));  // num_iterations
                psIns.setLong(6, rs.getLong(6));  // processing_time
                psIns.setFloat(7, rs.getFloat(7));  // deduction_amount
                psIns.setTimestamp(8, rs.getTimestamp(8));  // timestamp
                psIns.setString(9, rs.getString(9));  // viewable
                setBytes(psIns, 10, getBlobObject(rs, 10));  // received
                psIns.setInt(11, rs.getInt(11));  // succeeded
                psIns.setString(12, rs.getString(12));  // message

                retVal = psIns.executeUpdate();
                count += retVal;
                if (retVal != 1) {
                    throw new SQLException("TCLoadRound: Insert for coder_id " +
                            coder_id + ", round_id " + round_id +
                            ", problem_id " + problem_id +
                            ", test_case_id " + test_case_id +
                            " modified " + retVal + " rows, not one.");
                }

                printLoadProgress(count, "system_test_result");
            }

            log.info("System_test_result records copied = " + count);
        } catch (SQLException sqle) {
            DBMS.printSqlException(true, sqle);
            throw new Exception("Load of 'system_test_result' table failed.\n" +
                    sqle.getMessage());
        } finally {
            close(rs);
            close(psSel);
            close(psIns);
            close(psDel);
        }
    }

    /**
     * This loads the 'contest' table
     */
    private void loadContest() throws Exception {
        int retVal = 0;
        int count = 0;
        PreparedStatement psSel = null;
        PreparedStatement psIns = null;
        PreparedStatement psSel2 = null;
        PreparedStatement psUpd = null;
        ResultSet rs = null;
        ResultSet rs2 = null;
        StringBuffer query = null;

        try {
            query = new StringBuffer(100);
            query.append("SELECT c.contest_id ");    // 1
            query.append("       ,c.name ");         // 2
            query.append("       ,c.start_date ");   // 3
            query.append("       ,c.end_date ");     // 4
            query.append("       ,c.status ");       // 5
            query.append("       ,c.group_id ");     // 6
            query.append("       ,c.region_code ");  // 7
            query.append("       ,c.ad_text ");      // 8
            query.append("       ,c.ad_start ");     // 9
            query.append("       ,c.ad_end ");       // 10
            query.append("       ,c.ad_task ");      // 11
            query.append("       ,c.ad_command ");   // 12
            query.append("  FROM contest c ");
            query.append("       ,round r ");
            query.append(" WHERE r.round_id = ? ");
            query.append("   AND r.contest_id = c.contest_id");
            psSel = prepareStatement(query.toString(), SOURCE_DB);

            query = new StringBuffer(100);
            query.append("INSERT INTO contest ");
            query.append("      (contest_id ");    // 1
            query.append("       ,name ");         // 2
            query.append("       ,start_date ");   // 3
            query.append("       ,end_date ");     // 4
            query.append("       ,status ");       // 5
            query.append("       ,group_id ");     // 6
            query.append("       ,region_code ");  // 7
            query.append("       ,ad_text ");      // 8
            query.append("       ,ad_start ");     // 9
            query.append("       ,ad_end ");       // 10
            query.append("       ,ad_task ");      // 11
            query.append("       ,ad_command) ");  // 12
            query.append("VALUES (");
            query.append("?,?,?,?,?,?,?,?,?,?,");  // 10 values
            query.append("?,?)");                  // 12 total values
            psIns = prepareStatement(query.toString(), TARGET_DB);

            query = new StringBuffer(100);
            query.append("UPDATE contest ");
            query.append("   SET name = ? ");          // 1
            query.append("       ,start_date = ? ");   // 2
            query.append("       ,end_date = ? ");     // 3
            query.append("       ,status = ? ");       // 4
            query.append("       ,group_id = ? ");     // 5
            query.append("       ,region_code = ? ");  // 6
            query.append("       ,ad_text = ? ");      // 7
            query.append("       ,ad_start = ? ");     // 8
            query.append("       ,ad_end = ? ");       // 9
            query.append("       ,ad_task = ? ");      // 10
            query.append("       ,ad_command = ? ");  // 11
            query.append(" WHERE contest_id = ? ");    // 12
            psUpd = prepareStatement(query.toString(), TARGET_DB);

            query = new StringBuffer(100);
            query.append("SELECT 'pops' ");
            query.append("  FROM contest ");
            query.append(" WHERE contest_id = ?");
            psSel2 = prepareStatement(query.toString(), TARGET_DB);

            // On to the load
            psSel.setInt(1, fRoundId);
            rs = psSel.executeQuery();

            while (rs.next()) {
                int contest_id = rs.getInt(1);
                psSel2.clearParameters();
                psSel2.setInt(1, contest_id);
                rs2 = psSel2.executeQuery();

                // If next() returns true that means this row exists. If so,
                // we update. Otherwise, we insert.
                if (rs2.next()) {
                    psUpd.clearParameters();
                    psUpd.setString(1, rs.getString(2));  // name
                    psUpd.setTimestamp(2, rs.getTimestamp(3));  // start_date
                    psUpd.setTimestamp(3, rs.getTimestamp(4));  // end_date
                    psUpd.setString(4, rs.getString(5));  // status
                    psUpd.setInt(5, rs.getInt(6));  // group_id
                    psUpd.setString(6, rs.getString(7));  // region_code
                    psUpd.setString(7, rs.getString(8));  // ad_text
                    psUpd.setTimestamp(8, rs.getTimestamp(9));  // ad_start
                    psUpd.setTimestamp(9, rs.getTimestamp(10));  // ad_end
                    psUpd.setString(10, rs.getString(11));  // ad_task
                    psUpd.setString(11, rs.getString(12));  // ad_command
                    psUpd.setInt(12, rs.getInt(1));  // contest_id

                    retVal = psUpd.executeUpdate();
                    count += retVal;
                    if (retVal != 1) {
                        throw new SQLException("TCLoadRound: Insert for contest_id " +
                                contest_id +
                                " modified " + retVal + " rows, not one.");
                    }
                } else {
                    psIns.clearParameters();
                    psIns.setInt(1, rs.getInt(1));  // contest_id
                    psIns.setString(2, rs.getString(2));  // name
                    psIns.setTimestamp(3, rs.getTimestamp(3));  // start_date
                    psIns.setTimestamp(4, rs.getTimestamp(4));  // end_date
                    psIns.setString(5, rs.getString(5));  // status
                    psIns.setInt(6, rs.getInt(6));  // group_id
                    psIns.setString(7, rs.getString(7));  // region_code
                    psIns.setString(8, rs.getString(8));  // ad_text
                    psIns.setTimestamp(9, rs.getTimestamp(9));  // ad_start
                    psIns.setTimestamp(10, rs.getTimestamp(10));  // ad_end
                    psIns.setString(11, rs.getString(11));  // ad_task
                    psIns.setString(12, rs.getString(12));  // ad_command

                    retVal = psIns.executeUpdate();
                    count += retVal;
                    if (retVal != 1) {
                        throw new SQLException("TCLoadRound: Insert for contest_id " +
                                contest_id +
                                " modified " + retVal + " rows, not one.");
                    }
                }

                close(rs2);
                printLoadProgress(count, "contest");
            }

            log.info("Contest records copied = " + count);
        } catch (SQLException sqle) {
            DBMS.printSqlException(true, sqle);
            throw new Exception("Load of 'contest' table failed.\n" +
                    sqle.getMessage());
        } finally {
            close(rs);
            close(rs2);
            close(psSel);
            close(psSel2);
            close(psIns);
            close(psUpd);
        }
    }

    /**
     * This loads the 'problem' table
     */
    private void loadProblem() throws Exception {
        int retVal = 0;
        int count = 0;
        PreparedStatement psSel = null;
        PreparedStatement psSel2 = null;
        PreparedStatement psIns = null;
        PreparedStatement psUpd = null;
        ResultSet rs = null;
        ResultSet rs2 = null;
        StringBuffer query = null;

        try {
            query = new StringBuffer(100);
            query.append("SELECT p.problem_id ");                             // 1
            query.append("       ,rp.round_id ");                             // 2
            query.append("       ,c.result_type_id ");                        // 3
            query.append("       ,c.method_name ");                           // 4
            query.append("       ,c.class_name ");                            // 5
            query.append("       ,p.status_id ");                                // 6
            query.append("       ,c.default_solution ");                      // 7
            query.append("       ,c.component_text ");                          // 8
            query.append("       ,CURRENT ");                                 // 9
            query.append("       ,(SELECT data_type_desc ");                  // 10
            query.append("           FROM data_type ");
            query.append("          WHERE data_type_id = c.result_type_id) ");
            query.append("       ,d.difficulty_id ");                         // 11
            query.append("       ,d.difficulty_desc ");                       // 12
            query.append("       ,rp.division_id ");                          // 13
            query.append("       ,rp.points ");                               // 14
            query.append("  FROM problem p ");
            query.append("       ,round_component rp ");
            query.append("       ,difficulty d ");
            query.append("       ,component c ");
            query.append(" WHERE rp.round_id = ? ");
            query.append("   AND p.problem_id = c.problem_id");
            query.append("   AND c.component_id = rp.component_id ");
            query.append("   AND rp.difficulty_id = d.difficulty_id");
            psSel = prepareStatement(query.toString(), SOURCE_DB);

            query = new StringBuffer(100);
            query.append("INSERT INTO problem ");
            query.append("      (problem_id ");         // 1
            query.append("       ,round_id ");          // 2
            query.append("       ,result_type_id ");    // 3
            query.append("       ,method_name ");       // 4
            query.append("       ,class_name ");        // 5
            query.append("       ,status ");            // 6
            query.append("       ,default_solution ");  // 7
            query.append("       ,problem_text ");      // 8
            query.append("       ,modify_date ");       // 9
            query.append("       ,result_type_desc ");  // 10
            query.append("       ,level_id ");          // 11
            query.append("       ,level_desc ");        // 12
            query.append("       ,division_id ");       // 13
            query.append("       ,points ");            // 14
            query.append("       ,viewable) ");         // 15
            query.append("VALUES (");
            query.append("?,?,?,?,?,?,?,?,?,?,");
            query.append("?,?,?,?,?)");
            psIns = prepareStatement(query.toString(), TARGET_DB);

            query = new StringBuffer(100);
            query.append("UPDATE problem ");
            query.append("   SET result_type_id = ? ");     // 1
            query.append("       ,method_name = ? ");       // 2
            query.append("       ,class_name = ? ");        // 3
            query.append("       ,status = ? ");            // 4
            query.append("       ,default_solution = ? ");  // 5
            query.append("       ,problem_text = ? ");      // 6
            query.append("       ,modify_date = ? ");       // 7
            query.append("       ,result_type_desc = ? ");  // 8
            query.append("       ,level_id = ? ");          // 9
            query.append("       ,level_desc = ? ");        // 10
            query.append("       ,points = ? ");            // 11
            query.append("       ,viewable = ?");           // 12
            query.append(" WHERE problem_id = ? ");         // 13
            query.append("   AND round_id = ? ");           // 14
            query.append("   AND division_id = ? ");        // 15
            psUpd = prepareStatement(query.toString(), TARGET_DB);

            query = new StringBuffer(100);
            query.append("SELECT 'pops' FROM problem ");
            query.append(" WHERE problem_id = ? ");
            query.append("   AND round_id = ?");
            query.append("   AND division_id = ?");
            psSel2 = prepareStatement(query.toString(), TARGET_DB);

            // On to the load
            psSel.setInt(1, fRoundId);
            rs = psSel.executeQuery();

            while (rs.next()) {
                int problem_id = rs.getInt(1);
                int round_id = rs.getInt(2);
                int division_id = rs.getInt(13);

                psSel2.clearParameters();
                psSel2.setInt(1, problem_id);
                psSel2.setInt(2, round_id);
                psSel2.setInt(3, division_id);
                rs2 = psSel2.executeQuery();

                // If next() returns true that means this row exists. If so,
                // we update. Otherwise, we insert.
                if (rs2.next()) {
                    psUpd.clearParameters();
                    psUpd.setInt(1, rs.getInt(3));  // result_type_id
                    psUpd.setString(2, rs.getString(4));  // method_name
                    psUpd.setString(3, rs.getString(5));  // class_name
                    psUpd.setInt(4, rs.getInt(6));  // status
                    setBytes(psUpd, 5, getBytes(rs, 7));  // default_solution
                    setBytes(psUpd, 6, getBytes(rs, 8));  // problem_text
                    psUpd.setTimestamp(7, rs.getTimestamp(9));  // modify_date
                    psUpd.setString(8, rs.getString(10));  // result_type_desc
                    psUpd.setInt(9, rs.getInt(11));  // level_id
                    psUpd.setString(10, rs.getString(12));  // level_desc
                    psUpd.setFloat(11, rs.getFloat(14)); // points
                    psUpd.setInt(12, 1); //viewable
                    psUpd.setInt(13, rs.getInt(1));  // problem_id
                    psUpd.setInt(14, rs.getInt(2));  // round_id
                    psUpd.setInt(15, rs.getInt(13));  // division_id

                    retVal = psUpd.executeUpdate();
                    count += retVal;
                    if (retVal != 1) {
                        throw new SQLException("TCLoadRound: Update for problem_id " +
                                problem_id + ", round_id " + round_id +
                                " modified " + retVal + " rows, not one.");
                    }
                } else {
                    psIns.clearParameters();
                    psIns.setInt(1, rs.getInt(1));  // problem_id
                    psIns.setInt(2, rs.getInt(2));  // round_id
                    psIns.setInt(3, rs.getInt(3));  // result_type_id
                    psIns.setString(4, rs.getString(4));  // method_name
                    psIns.setString(5, rs.getString(5));  // class_name
                    psIns.setInt(6, rs.getInt(6));  // status
                    setBytes(psIns, 7, getBytes(rs, 7));  // default_solution
                    setBytes(psIns, 8, getBytes(rs, 8));  // problem_text
                    psIns.setTimestamp(9, rs.getTimestamp(9));  // modify_date
                    psIns.setString(10, rs.getString(10));  // result_type_desc
                    psIns.setInt(11, rs.getInt(11));  // level_id
                    psIns.setString(12, rs.getString(12));  // level_desc
                    psIns.setInt(13, rs.getInt(13));  // division_id
                    psIns.setFloat(14, rs.getFloat(14)); //points
                    psIns.setInt(15, 1); //viewable

                    retVal = psIns.executeUpdate();
                    count += retVal;
                    if (retVal != 1) {
                        throw new SQLException("TCLoadRound: Insert for problem_id " +
                                problem_id + ", round_id " + round_id +
                                " modified " + retVal + " rows, not one.");
                    }
                }

                close(rs2);
                printLoadProgress(count, "problem");
            }

            log.info("Problem records copied = " + count);
        } catch (SQLException sqle) {
            DBMS.printSqlException(true, sqle);
            throw new Exception("Load of 'problem' table failed.\n" +
                    sqle.getMessage());
        } finally {
            close(rs);
            close(rs2);
            close(psSel);
            close(psSel2);
            close(psIns);
            close(psUpd);
        }
    }

    /**
     * This loads the 'round' table
     */
    private void loadRound() throws Exception {
        int retVal = 0;
        int count = 0;
        PreparedStatement psSel = null;
        PreparedStatement psSel2 = null;
        PreparedStatement psIns = null;
        PreparedStatement psUpd = null;
        ResultSet rs = null;
        ResultSet rs2 = null;
        StringBuffer query = null;

        try {
            query = new StringBuffer(100);
            query.append("SELECT r.round_id ");                          // 1
            query.append("       ,r.contest_id ");                       // 2
            query.append("       ,r.name ");                             // 3
            query.append("       ,r.status ");                           // 4
            query.append("       ,(SELECT sum(paid) ");                  // 5
            query.append("           FROM room_result rr ");
            query.append("          WHERE rr.round_id = r.round_id) ");
            query.append("       ,rs.start_time ");                      // 6
            query.append("       ,r.round_type_id ");                    // 7
            query.append("       ,r.invitational ");                     // 8
            query.append("       ,r.notes ");                            // 9
            query.append("       ,(SELECT rtlu.round_type_desc ");       // 10
            query.append("           FROM round_type_lu rtlu ");
            query.append("          WHERE rtlu.round_type_id = r.round_type_id) ");
            query.append("  FROM round r ");
            query.append("       ,round_segment rs ");
            query.append(" WHERE r.round_id = ? ");
            query.append("   AND rs.round_id = r.round_id ");
            query.append("   AND rs.segment_id = " + CODING_SEGMENT_ID);
            psSel = prepareStatement(query.toString(), SOURCE_DB);

            // We have 8 values in the insert as opposed to 7 in the select
            // because we want to provide a default value for failed. We
            // don't have a place to select failed from in the transactional
            // DB
            query = new StringBuffer(100);
            query.append("INSERT INTO round ");
            query.append("      (round_id ");          // 1
            query.append("       ,contest_id ");       // 2
            query.append("       ,name ");             // 3
            query.append("       ,status ");           // 4
            query.append("       ,money_paid ");       // 5
            query.append("       ,calendar_id ");      // 6
            query.append("       ,failed ");           // 7
            query.append("       ,round_type_id ");    // 8
            query.append("       ,invitational  ");    // 9
            query.append("       ,notes         ");    // 10
            query.append("       ,round_type_desc) "); // 11
            query.append("VALUES (");
            query.append("?,?,?,?,?,?,?,?,?,?,");
            query.append("?)");

            psIns = prepareStatement(query.toString(), TARGET_DB);

            query = new StringBuffer(100);
            query.append("UPDATE round ");
            query.append("   SET contest_id = ? ");       // 1
            query.append("       ,name = ? ");            // 2
            query.append("       ,status = ? ");          // 3
            query.append("       ,money_paid = ? ");      // 4
            query.append("       ,calendar_id = ? ");     // 5
            query.append("       ,failed = ? ");          // 6
            query.append("       ,round_type_id = ? ");   // 7
            query.append("       ,invitational  = ? ");   // 8
            query.append("       ,notes = ?         ");   // 9
            query.append("       ,round_type_desc = ? "); // 10
            query.append(" WHERE round_id = ? ");         // 11
            psUpd = prepareStatement(query.toString(), TARGET_DB);

            query = new StringBuffer(100);
            query.append("SELECT 'pops' FROM round where round_id = ?");
            psSel2 = prepareStatement(query.toString(), TARGET_DB);

            // On to the load
            psSel.setInt(1, fRoundId);
            rs = psSel.executeQuery();
            while (rs.next()) {
                int round_id = rs.getInt(1);
                psSel2.clearParameters();
                psSel2.setInt(1, round_id);
                rs2 = psSel2.executeQuery();

                // Retrieve the calendar_id for the start_time of this round
                java.sql.Timestamp stamp = rs.getTimestamp(6);
                int calendar_id = lookupCalendarId(stamp, TARGET_DB);

                // If next() returns true that means this row exists. If so,
                // we update. Otherwise, we insert.
                if (rs2.next()) {
                    psUpd.clearParameters();
                    psUpd.setInt(1, rs.getInt(2));  // contest_id
                    psUpd.setString(2, rs.getString(3));  // name
                    psUpd.setString(3, rs.getString(4));  // status
                    psUpd.setFloat(4, rs.getFloat(5));  // money_paid
                    psUpd.setInt(5, calendar_id);         // cal_id of start_time
                    psUpd.setInt(6, 0);                   // failed (default is 0)
                    psUpd.setInt(7, rs.getInt(7));        // round_type_id
                    psUpd.setInt(8, rs.getInt(8));        // invitational
                    psUpd.setString(9, rs.getString(9));     // notes
                    psUpd.setString(10, rs.getString(10));    // round_type_desc
                    psUpd.setInt(11, rs.getInt(1));  // round_id

                    retVal = psUpd.executeUpdate();
                    count += retVal;
                    if (retVal != 1) {
                        throw new SQLException("TCLoadRound: Update for round_id " +
                                round_id +
                                " modified " + retVal + " rows, not one.");
                    }
                } else {
                    psIns.clearParameters();
                    psIns.setInt(1, rs.getInt(1));  // round_id
                    psIns.setInt(2, rs.getInt(2));  // contest_id
                    psIns.setString(3, rs.getString(3));  // name
                    psIns.setString(4, rs.getString(4));  // status
                    psIns.setFloat(5, rs.getFloat(5));  // money_paid
                    psIns.setInt(6, calendar_id);  // cal_id of start_time
                    psIns.setInt(7, 0);                   // failed (default is 0)
                    psIns.setInt(8, rs.getInt(7));        // round_type_id
                    psIns.setInt(9, rs.getInt(8));        // invitational
                    psIns.setString(10, rs.getString(9));     // notes
                    psIns.setString(11, rs.getString(10));    // round_type_desc

                    retVal = psIns.executeUpdate();
                    count += retVal;
                    if (retVal != 1) {
                        throw new SQLException("TCLoadRound: Insert for round_id " +
                                round_id +
                                " modified " + retVal + " rows, not one.");
                    }
                }

                close(rs2);
                printLoadProgress(count, "round");
            }

            log.info("Round records copied = " + count);
        } catch (SQLException sqle) {
            DBMS.printSqlException(true, sqle);
            throw new Exception("Load of 'round' table failed.\n" +
                    sqle.getMessage());
        } finally {
            close(rs2);
            close(psSel);
            close(psSel2);
            close(psIns);
            close(psUpd);
        }
    }

    /**
     * This loads the 'room' table
     */
    private void loadRoom() throws Exception {
        int retVal = 0;
        int count = 0;
        PreparedStatement psSel = null;
        PreparedStatement psSel2 = null;
        PreparedStatement psIns = null;
        PreparedStatement psUpd = null;
        ResultSet rs = null;
        ResultSet rs2 = null;
        StringBuffer query = null;

        try {
            query = new StringBuffer(100);
            query.append("SELECT r.room_id");                                    // 1
            query.append("       ,r.round_id ");                                 // 2
            query.append("       ,r.name ");                                     // 3
            query.append("       ,r.state_code ");                               // 4
            query.append("       ,(SELECT s.state_name ");                       // 5
            query.append("           FROM state s ");
            query.append("          WHERE s.state_code = r.state_code) ");
            query.append("       ,r.country_code ");                             // 6
            query.append("       ,(SELECT c.country_name ");                     // 7
            query.append("           FROM country c ");
            query.append("          WHERE c.country_code = r.country_code) ");
            query.append("       ,r.region_code ");                              // 8
            query.append("       ,(SELECT reg.region_name ");                    // 9
            query.append("           FROM region reg ");
            query.append("          WHERE reg.region_code = r.region_code ");
            query.append("            AND reg.country_code = r.country_code) ");
            query.append("       ,r.division_id ");                              // 10
            query.append("       ,(SELECT d.division_desc ");                    // 11
            query.append("           FROM division d ");
            query.append("          WHERE d.division_id = r.division_id) ");
            query.append("       ,r.room_type_id ");                             // 12
            query.append("       ,(SELECT rt.room_type_desc ");                  // 13
            query.append("           FROM room_type rt ");
            query.append("          WHERE rt.room_type_id = r.room_type_id) ");
            query.append("       ,r.eligible ");                                 // 14
            query.append("       ,r.unrated ");                                  // 15
            query.append("   FROM room r ");
            query.append("  WHERE round_id = ?");
            query.append("    AND r.room_type_id = " + CONTEST_ROOM);
            psSel = prepareStatement(query.toString(), SOURCE_DB);

            query = new StringBuffer(100);
            query.append("INSERT INTO room ");
            query.append("      (room_id ");           // 1
            query.append("       ,round_id ");         // 2
            query.append("       ,name ");             // 3
            query.append("       ,state_code ");       // 4
            query.append("       ,state_name ");       // 5
            query.append("       ,country_code ");     // 6
            query.append("       ,country_name ");     // 7
            query.append("       ,region_code ");      // 8
            query.append("       ,region_name ");      // 9
            query.append("       ,division_id ");      // 10
            query.append("       ,division_desc ");    // 11
            query.append("       ,room_type_id ");     // 12
            query.append("       ,room_type_desc ");   // 13
            query.append("       ,eligible ");         // 14
            query.append("       ,unrated) ");         // 15
            query.append("VALUES (");
            query.append("?,?,?,?,?,?,?,?,?,?,");  // 10 values
            query.append("?,?,?,?,?)");            // 15 total values
            psIns = prepareStatement(query.toString(), TARGET_DB);

            query = new StringBuffer(100);
            query.append("UPDATE room ");
            query.append("   SET round_id = ? ");          // 1
            query.append("       ,name = ? ");             // 2
            query.append("       ,state_code = ? ");       // 3
            query.append("       ,state_name = ? ");       // 4
            query.append("       ,country_code = ? ");     // 5
            query.append("       ,country_name = ? ");     // 6
            query.append("       ,region_code = ? ");      // 7
            query.append("       ,region_name = ? ");      // 8
            query.append("       ,division_id = ? ");      // 9
            query.append("       ,division_desc = ? ");    // 10
            query.append("       ,room_type_id = ? ");     // 11
            query.append("       ,room_type_desc = ? ");   // 12
            query.append("       ,eligible = ? ");         // 13
            query.append("       ,unrated = ? ");          // 14
            query.append(" WHERE room_id = ? ");           // 15
            psUpd = prepareStatement(query.toString(), TARGET_DB);

            query = new StringBuffer(100);
            query.append("SELECT 'pops' FROM room WHERE room_id = ?");
            psSel2 = prepareStatement(query.toString(), TARGET_DB);

            // On to the load
            psSel.setInt(1, fRoundId);
            rs = psSel.executeQuery();

            while (rs.next()) {
                int room_id = rs.getInt(1);
                psSel2.clearParameters();
                psSel2.setInt(1, room_id);
                rs2 = psSel2.executeQuery();

                // If next() returns true that means this row exists. If so,
                // we update. Otherwise, we insert.
                if (rs2.next()) {
                    psUpd.clearParameters();
                    psUpd.setInt(1, rs.getInt(2));  // round_id
                    psUpd.setString(2, rs.getString(3));  // name
                    psUpd.setString(3, rs.getString(4));  // state_code
                    psUpd.setString(4, rs.getString(5));  // state_name
                    psUpd.setString(5, rs.getString(6));  // country_code
                    psUpd.setString(6, rs.getString(7));  // country_name
                    psUpd.setString(7, rs.getString(8));  // region_code
                    psUpd.setString(8, rs.getString(9));  // region_name
                    psUpd.setInt(9, rs.getInt(10));  // division_id
                    psUpd.setString(10, rs.getString(11));  // division_desc
                    psUpd.setInt(11, rs.getInt(12));  // room_type_id
                    psUpd.setString(12, rs.getString(13));  // room_type_desc
                    psUpd.setInt(13, rs.getInt(14));  // eligible
                    psUpd.setInt(14, rs.getInt(15));  // unrated
                    psUpd.setInt(15, rs.getInt(1));  // room_id

                    retVal = psUpd.executeUpdate();
                    count += retVal;
                    if (retVal != 1) {
                        throw new SQLException("TCLoadRound: Update for room_id " +
                                room_id +
                                " modified " + retVal + " rows, not one.");
                    }
                } else {
                    psIns.clearParameters();
                    psIns.setInt(1, rs.getInt(1));  // room_id
                    psIns.setInt(2, rs.getInt(2));  // round_id
                    psIns.setString(3, rs.getString(3));  // name
                    psIns.setString(4, rs.getString(4));  // state_code
                    psIns.setString(5, rs.getString(5));  // state_name
                    psIns.setString(6, rs.getString(6));  // country_code
                    psIns.setString(7, rs.getString(7));  // country_name
                    psIns.setString(8, rs.getString(8));  // region_code
                    psIns.setString(9, rs.getString(9));  // region_name
                    psIns.setInt(10, rs.getInt(10));  // division_id
                    psIns.setString(11, rs.getString(11));  // division_desc
                    psIns.setInt(12, rs.getInt(12));  // room_type_id
                    psIns.setString(13, rs.getString(13));  // room_type_desc
                    psIns.setInt(14, rs.getInt(14));  // eligible
                    psIns.setInt(15, rs.getInt(15));  // unrated

                    retVal = psIns.executeUpdate();
                    count += retVal;
                    if (retVal != 1) {
                        throw new SQLException("TCLoadRound: Insert for room_id " +
                                room_id +
                                " modified " + retVal + " rows, not one.");
                    }
                }

                close(rs2);
                printLoadProgress(count, "room");
            }

            log.info("Room records copied = " + count);
        } catch (SQLException sqle) {
            DBMS.printSqlException(true, sqle);
            throw new Exception("Load of 'room' table failed.\n" +
                    sqle.getMessage());
        } finally {
            close(rs);
            close(rs2);
            close(psSel);
            close(psSel2);
            close(psIns);
            close(psUpd);
        }
    }

    /**
     * This loads the 'room_result'. This is actually a partial load of
     * the 'room_result' table as four columns are not populated:
     * school_points, submission_points, problems_submitted and
     * point_standard_deviation. We get these later on in the aggregate
     * load.

     important: dw:room_result.school_id comes from oltp:user_school_xref;
     if students change schools, reloading an old round will lose historical data

     */
    private void loadRoomResult() throws Exception {
        int retVal = 0;
        int count = 0;
        PreparedStatement psSel = null;
        PreparedStatement psIns = null;
        PreparedStatement psDel = null;
        ResultSet rs = null;
        StringBuffer query = null;

        try {
            query = new StringBuffer(100);
            query.append("SELECT rr.round_id ");                              // 1
            query.append("       ,rr.room_id ");                              // 2
            query.append("       ,rr.coder_id ");                             // 3
            query.append("       ,rr.point_total ");                          // 4
            query.append("       ,rr.room_seed ");                            // 5
            query.append("       ,rr.old_rating ");                           // 6
            query.append("       ,rr.new_rating ");                           // 7
            query.append("       ,rr.room_placed ");                          // 8
            query.append("       ,rr.attended ");                             // 9
            query.append("       ,rr.advanced ");                             // 10
            query.append("       ,(SELECT sum(c.challenger_points) ");        // 11
            query.append("           FROM challenge c ");
            query.append("          WHERE c.round_id = rr.round_id ");
            query.append("          AND c.status_id <> " + CHALLENGE_NULLIFIED);
            query.append("          AND c.challenger_id = rr.coder_id) ");
            query.append("       ,(SELECT sum(deduction_amount) ");           // 12
            query.append("           FROM system_test_result str ");
            query.append("          WHERE str.round_id = rr.round_id ");
            query.append("            AND str.coder_id = rr.coder_id) ");
            query.append("       ,(SELECT division_id FROM room ");           // 13
            query.append("          WHERE room.room_id = rr.room_id) ");
            query.append("       ,(SELECT count(*) ");                        // 14
            query.append("           FROM round_component rp ");
            query.append("                ,room r ");
            query.append("          WHERE rp.round_id = rr.round_id ");
            query.append("            AND rp.division_id = r.division_id ");
            query.append("            AND rr.room_id = r.room_id) ");
            query.append("       ,(SELECT count(*) FROM component_state cs ");  // 15
            query.append("          WHERE cs.round_id = rr.round_id ");
            query.append("            AND cs.coder_id = rr.coder_id ");
            query.append("            AND cs.status_id = " + STATUS_PASSED_SYS_TEST + ") ");
            query.append("       ,(SELECT count(*) FROM component_state cs ");  // 16
            query.append("          WHERE cs.round_id = rr.round_id ");
            query.append("            AND cs.coder_id = rr.coder_id ");
            query.append("            AND cs.status_id = " + STATUS_FAILED_SYS_TEST + ") ");
            query.append("       ,(SELECT count(*) FROM challenge c ");       // 17
            query.append("          WHERE c.round_id = rr.round_id ");
            query.append("          AND c.status_id <> " + CHALLENGE_NULLIFIED);
            query.append("            AND c.defendant_id = rr.coder_id ");
            query.append("            AND c.succeeded = " + STATUS_SUCCEEDED + ") ");
            query.append("       ,(SELECT count(*) FROM component_state cs ");  // 18
            query.append("          WHERE cs.round_id = rr.round_id ");
            query.append("            AND cs.coder_id = rr.coder_id) ");
            query.append("       ,(SELECT count(*) FROM component_state cs ");  // 19
            query.append("          WHERE cs.round_id = rr.round_id ");
            query.append("            AND cs.coder_id = rr.coder_id ");
            query.append("            AND cs.status_id = " + STATUS_OPENED + ") ");
            query.append("       ,(SELECT count(*) FROM challenge c ");       // 20
            query.append("          WHERE c.challenger_id = rr.coder_id ");
            query.append("          AND c.status_id <> " + CHALLENGE_NULLIFIED);
            query.append("            AND c.round_id = rr.round_id) ");
            query.append("       ,(SELECT count(*) FROM challenge c ");       // 21
            query.append("          WHERE c.challenger_id = rr.coder_id ");
            query.append("          AND c.status_id <> " + CHALLENGE_NULLIFIED);
            query.append("            AND c.round_id = rr.round_id ");
            query.append("            AND c.succeeded = " + STATUS_SUCCEEDED + ") ");
            query.append("       ,(SELECT count(*) FROM challenge c ");       // 22
            query.append("          WHERE c.challenger_id = rr.coder_id ");
            query.append("          AND c.status_id <> " + CHALLENGE_NULLIFIED);
            query.append("            AND c.round_id = rr.round_id ");
            query.append("            AND c.succeeded = " + STATUS_FAILED + ") ");
            query.append("       ,(SELECT count(*) FROM challenge c ");       // 23
            query.append("          WHERE c.defendant_id = rr.coder_id ");
            query.append("          AND c.status_id <> " + CHALLENGE_NULLIFIED);
            query.append("            AND c.round_id = rr.round_id) ");
            query.append("       ,(SELECT count(*) FROM challenge c ");       // 24
            query.append("          WHERE c.defendant_id = rr.coder_id ");
            query.append("          AND c.status_id <> " + CHALLENGE_NULLIFIED);
            query.append("            AND c.round_id = rr.round_id ");
            query.append("            AND c.succeeded = " + STATUS_SUCCEEDED + ") ");
            query.append("       ,(SELECT count(*) FROM challenge c ");       // 25
            query.append("          WHERE c.defendant_id = rr.coder_id ");
            query.append("          AND c.status_id <> " + CHALLENGE_NULLIFIED);
            query.append("            AND c.round_id = rr.round_id ");
            query.append("            AND c.succeeded = " + STATUS_FAILED + ") ");
            query.append("       ,(SELECT sum(c.defendant_points) ");           // 26
            query.append("           FROM challenge c ");
            query.append("          WHERE c.round_id = rr.round_id ");
            query.append("          AND c.status_id <> " + CHALLENGE_NULLIFIED);
            query.append("            AND c.defendant_id = rr.coder_id) ");
            query.append("       ,rr.overall_rank ");                        // 27
            query.append("       ,rr.division_placed ");                     // 28
            query.append("       ,rr.division_seed ");                       // 29
            query.append("       , x.school_id");                             //30
            query.append("  FROM room_result rr ");
            query.append("  JOIN room r ON rr.round_id = r.round_id ");
            query.append("   AND rr.room_id = r.room_id ");
            query.append("  JOIN user_school_xref x ON rr.coder_id = x.user_id");
            query.append("   AND x.current_ind = 1");
            query.append(" WHERE r.room_type_id = " + CONTEST_ROOM);
            query.append("   AND rr.round_id = ?");
            query.append("   AND rr.attended = 'Y'");
            query.append("   AND EXISTS ");
            query.append("     (SELECT 'dok' ");
            query.append("        FROM user_group_xref ugx");
            query.append("       WHERE ugx.login_id = rr.coder_id");
            query.append("         AND ugx.group_id = " + STUDENT_GROUP_ID + ")");

            psSel = prepareStatement(query.toString(), SOURCE_DB);

            query = new StringBuffer(1024);
            query.append("INSERT INTO room_result ");
            query.append("      (round_id ");                         // 1
            query.append("       ,room_id ");                         // 2
            query.append("       ,coder_id ");                        // 3
            query.append("       ,final_points ");                    // 4
            query.append("       ,room_seed ");                       // 5
            query.append("       ,old_rating ");                      // 6
            query.append("       ,new_rating ");                      // 7
            query.append("       ,room_placed ");                     // 8
            query.append("       ,attended ");                        // 9
            query.append("       ,advanced ");                        // 10
            query.append("       ,challenge_points ");                // 11
            query.append("       ,system_test_points ");              // 12
            query.append("       ,division_id ");                     // 13
            query.append("       ,problems_presented ");              // 14
            query.append("       ,problems_correct ");                // 15
            query.append("       ,problems_failed_by_system_test ");  // 16
            query.append("       ,problems_failed_by_challenge ");    // 17
            query.append("       ,problems_opened ");                 // 18
            query.append("       ,problems_left_open ");              // 19
            query.append("       ,challenge_attempts_made ");         // 20
            query.append("       ,challenges_made_successful ");      // 21
            query.append("       ,challenges_made_failed ");          // 22
            query.append("       ,challenge_attempts_received ");     // 23
            query.append("       ,challenges_received_successful ");  // 24
            query.append("       ,challenges_received_failed ");      // 25
            query.append("       ,defense_points ");                  // 26
            query.append("       ,overall_rank ");                    // 27
            query.append("       ,division_placed ");                 // 28
            query.append("       ,division_seed ");                   // 29
            query.append("       ,school_id) ");                      // 30
            query.append("VALUES (?,?,?,?,?,?,?,?,?,?,");  // 10 values
            query.append("        ?,?,?,?,?,?,?,?,?,?,");  // 20 values
            query.append("        ?,?,?,?,?,?,?,?,?,?)");  // 30 total values
            psIns = prepareStatement(query.toString(), TARGET_DB);

            query = new StringBuffer(1024);
            query.append("DELETE FROM room_result ");
            query.append(" WHERE round_id = ? ");
            query.append("   AND room_id = ? ");
            query.append("   AND coder_id = ? ");
            psDel = prepareStatement(query.toString(), TARGET_DB);

            // On to the load
            psSel.setInt(1, fRoundId);
            rs = psSel.executeQuery();

            while (rs.next()) {
                int round_id = rs.getInt(1);
                int room_id = rs.getInt(2);
                int coder_id = rs.getInt(3);

                psDel.clearParameters();
                psDel.setInt(1, round_id);
                psDel.setInt(2, room_id);
                psDel.setInt(3, coder_id);
                psDel.executeUpdate();

                psIns.clearParameters();
                psIns.setInt(1, rs.getInt(1));  // round_id
                psIns.setInt(2, rs.getInt(2));  // room_id
                psIns.setInt(3, rs.getInt(3));  // coder_id
                psIns.setFloat(4, rs.getFloat(4));  // point_total
                psIns.setInt(5, rs.getInt(5));  // room_seed
                psIns.setInt(6, rs.getInt(6));  // old_rating
                psIns.setInt(7, rs.getInt(7));  // new_rating
                psIns.setInt(8, rs.getInt(8));  // room_placed
                psIns.setString(9, rs.getString(9));  // attended
                psIns.setString(10, rs.getString(10));  // advanced
                psIns.setFloat(11, rs.getFloat(11));  // challenge_points
                psIns.setFloat(12, rs.getFloat(12));  // system_test_points
                psIns.setInt(13, rs.getInt(13));  // division_id
                psIns.setInt(14, rs.getInt(14));  // problems_presented
                psIns.setInt(15, rs.getInt(15));  // problems_correct
                psIns.setInt(16, rs.getInt(16));  // problems_failed_by_system_test
                psIns.setInt(17, rs.getInt(17));  // problems_failed_by_challenge
                psIns.setInt(18, rs.getInt(18));  // problems_opened
                psIns.setInt(19, rs.getInt(19));  // problems_left_open
                psIns.setInt(20, rs.getInt(20));  // challenge_attempts_made
                psIns.setInt(21, rs.getInt(21));  // challenges_made_successful
                psIns.setInt(22, rs.getInt(22));  // challenges_made_failed
                psIns.setInt(23, rs.getInt(23));  // challenge_attempts_received
                psIns.setInt(24, rs.getInt(24));  // challenges_received_successful
                psIns.setInt(25, rs.getInt(25));  // challenges_received_failed
                psIns.setFloat(26, rs.getFloat(26));  // defense_points
                psIns.setInt(27, rs.getInt(27));  // overall_rank
                psIns.setInt(28, rs.getInt(28));  // division_placed
                psIns.setInt(29, rs.getInt(29));  // division_seed
                psIns.setInt(30, rs.getInt(30));  // school_id
                retVal = psIns.executeUpdate();
                count += retVal;
                if (retVal != 1) {
                    throw new SQLException("TCLoadRound: Insert for coder_id " +
                            coder_id + ", round_id " + round_id +
                            ", room_id " + room_id +
                            " modified " + retVal + " rows, not one.");
                }

                printLoadProgress(count, "room_result");
            }

            log.info("Room_result records copied = " + count);
        } catch (SQLException sqle) {
            DBMS.printSqlException(true, sqle);
            throw new Exception("Load of 'room_result' table failed.\n" +
                    sqle.getMessage());
        } finally {
            close(rs);
            close(psSel);
            close(psIns);
            close(psDel);
        }
    }

    /**
     * This loads the 'coder_problem' table. This is a new table
     * which reports the results of each problem by coder.
     */
    private void loadCoderProblem() throws Exception {
        int retVal = 0;
        int count = 0;
        PreparedStatement psSel = null;
        PreparedStatement psSelOpenSubmitOrder = null;
        PreparedStatement psSel2 = null;
        PreparedStatement psIns = null;
        PreparedStatement psDel = null;
        ResultSet rs = null;
        ResultSet rs2 = null;
        StringBuffer query = null;

        int coder_id = 0;
        int round_id = 0;
        int division_id = 0;
        int problem_id = 0;
        int component_id = 0;

        try {
            query = new StringBuffer(100);
            query.append("SELECT cs.coder_id ");                                 // 1
            query.append("       ,cs.round_id ");                                // 2
            // 3: division_id
            query.append("       ,(SELECT r.division_id ");                      // 3
            query.append("           FROM room r ");
            query.append("                ,room_result rr ");
            query.append("          WHERE rr.coder_id = cs.coder_id ");
            query.append("            AND rr.round_id = cs.round_id ");
            query.append("            AND r.room_type_id = " + CONTEST_ROOM);
            query.append("            AND r.room_id = rr.room_id) ");
            query.append("       ,(SELECT comp.problem_id FROM component comp WHERE cs.component_id = comp.component_id)");                              // 4
            query.append("       ,s.submission_points ");                        // 5
            query.append("       ,cs.points ");                                  // 6
            query.append("       ,cs.status_id ");                               // 7
            // 8: end_status_text
            query.append("       ,(SELECT status_desc ");                        // 8
            query.append("           FROM component_status_lu ");
            query.append("          WHERE component_status_id = cs.status_id) ");
            query.append("       ,c.open_time ");                                // 9
            query.append("       ,s.submit_time ");                              // 10
            query.append("       ,s.submit_time - c.open_time ");                // 11
            query.append("       ,cs.language_id ");                             // 12
            // 13: challenge_points
            query.append("       ,(SELECT sum(c.challenger_points) ");           // 13
            query.append("           FROM challenge c ");
            query.append("          WHERE c.round_id = cs.round_id ");
            query.append("          AND c.status_id <> " + CHALLENGE_NULLIFIED);
            query.append("            AND c.challenger_id = cs.coder_id ");
            query.append("            AND c.component_id = cs.component_id) ");
            // 14: system_test_points
            query.append("       ,(SELECT sum(deduction_amount) ");              // 14
            query.append("           FROM system_test_result str ");
            query.append("          WHERE str.round_id = cs.round_id ");
            query.append("            AND str.coder_id = cs.coder_id ");
            query.append("            AND str.component_id = cs.component_id) ");
            // 15: defense_points
            query.append("       ,(SELECT sum(defendant_points) ");              // 15
            query.append("           FROM challenge c ");
            query.append("          WHERE c.round_id = cs.round_id ");
            query.append("          AND c.status_id <> " + CHALLENGE_NULLIFIED);
            query.append("            AND c.defendant_id = cs.coder_id ");
            query.append("            AND c.component_id = cs.component_id) ");
            query.append("       ,(SELECT rs.end_time");                         // 16
            query.append("           FROM round_segment rs");
            query.append("          WHERE rs.round_id = cs.round_id");
            query.append("            AND rs.segment_id = 2)");                  // coding segment...need constant
            query.append("       ,cs.component_id ");
            query.append(" FROM component_state cs");
            query.append(" LEFT OUTER JOIN submission s ");
            query.append(" ON cs.component_state_id = s.component_state_id");
            query.append(" AND s.submission_number = cs.submission_number");
            query.append(" LEFT OUTER JOIN compilation c ");
            query.append(" ON cs.component_state_id = c.component_state_id");
            query.append(" JOIN room_result rr ");
            query.append(" ON rr.round_id = cs.round_id");
            query.append(" AND rr.coder_id = cs.coder_id");
            query.append(" WHERE cs.round_id = ?");
            query.append("   AND rr.attended = 'Y'");
            query.append("   AND EXISTS ");
            query.append("     (SELECT 'dok' ");
            query.append("        FROM user_group_xref ugx");
            query.append("       WHERE ugx.login_id = cs.coder_id");
            query.append("         AND ugx.group_id = " + STUDENT_GROUP_ID + ")");

            psSel = prepareStatement(query.toString(), SOURCE_DB);

            query = new StringBuffer(100);
            query.append("SELECT rp.open_order ");     // 1
            query.append("       ,rp.submit_order ");  // 2
            query.append("  FROM round_component rp ");
            query.append(" WHERE rp.component_id = ? ");
            query.append("   AND rp.round_id = ? ");
            query.append("   AND rp.division_id = ? ");
            psSelOpenSubmitOrder = prepareStatement(query.toString(), SOURCE_DB);

            query = new StringBuffer(100);
            query.append("SELECT rp.difficulty_id ");    // 1
            query.append("       ,d.difficulty_desc ");  // 2
            query.append("  FROM round_component rp ");
            query.append("       ,difficulty d ");
            query.append(" WHERE rp.component_id = ? ");
            query.append("   AND rp.division_id = ? ");
            query.append("   AND rp.round_id = ? ");
            query.append("   AND rp.difficulty_id = d.difficulty_id ");
            psSel2 = prepareStatement(query.toString(), SOURCE_DB);

            // Need to add these in later if we determine we need them
            query = new StringBuffer(100);
            query.append("INSERT INTO coder_problem ");
            query.append("      (coder_id ");             // 1
            query.append("       ,round_id ");            // 2
            query.append("       ,division_id ");         // 3
            query.append("       ,problem_id ");          // 4
            query.append("       ,submission_points ");   // 5
            query.append("       ,final_points ");        // 6
            query.append("       ,end_status_id ");       // 7
            query.append("       ,end_status_text ");     // 8
            query.append("       ,open_time ");           // 9
            query.append("       ,submit_time ");         // 10
            query.append("       ,time_elapsed ");        // 11
            query.append("       ,language_id ");         // 12
            query.append("       ,challenge_points ");    // 13
            query.append("       ,system_test_points ");  // 14
            query.append("       ,defense_points ");      // 15
            query.append("       ,open_order ");          // 16
            query.append("       ,submit_order ");        // 17
            query.append("       ,level_id ");            // 18
            query.append("       ,level_desc) ");         // 19
            query.append("VALUES (");
            query.append("?,?,?,?,?,?,?,?,?,?,");  // 10 values
            query.append("?,?,?,?,?,?,?,?,?)");    // 19 total values
            psIns = prepareStatement(query.toString(), TARGET_DB);

            query = new StringBuffer(100);
            query.append("DELETE FROM coder_problem ");
            query.append(" WHERE coder_id = ? ");
            query.append("   AND round_id = ? ");
            query.append("   AND division_id = ? ");
            query.append("   AND problem_id = ?");
            psDel = prepareStatement(query.toString(), TARGET_DB);

            // On to the load
            psSel.setInt(1, fRoundId);
            rs = psSel.executeQuery();


            while (rs.next()) {
                coder_id = rs.getInt(1);
                round_id = rs.getInt(2);
                division_id = rs.getInt(3);
                problem_id = rs.getInt(4);
                component_id = rs.getInt("component_id");
                // if they didn't submit, use the difference between open time and the end of the coding phase
                // otherwise use the difference between open time and submit time
                long elapsed_time = rs.getLong(10) == 0 ? rs.getDate(16).getTime() - rs.getLong(9) : rs.getLong(11);

                psSel2.clearParameters();
                psSel2.setInt(1, component_id);
                psSel2.setInt(2, division_id);
                psSel2.setInt(3, fRoundId);
                int level_id = -1;
                String level_desc = null;
                rs2 = psSel2.executeQuery();

                // Get level_id and level_desc
                if (rs2.next()) {
                    level_id = rs2.getInt(1);
                    level_desc = rs2.getString(2);
                } else {
                    throw new SQLException("Unable to find level_id and level_desc " +
                            "for problem_id " + problem_id +
                            " and division_id " + division_id);
                }

                close(rs2);

                // Get open_order and submit_order
                psSelOpenSubmitOrder.clearParameters();
                psSelOpenSubmitOrder.setInt(1, component_id);
                psSelOpenSubmitOrder.setInt(2, fRoundId);
                psSelOpenSubmitOrder.setInt(3, division_id);
                rs2 = psSelOpenSubmitOrder.executeQuery();

                int open_order = 0, submit_order = 0;
                if (rs2.next()) {
                    open_order = rs2.getInt(1);
                    submit_order = rs2.getInt(2);
                } else {
                    throw new SQLException("Unable to find open_order and submit_order " +
                            "for problem_id " + problem_id +
                            " and division_id " + division_id);
                }

                close(rs2);

                psDel.clearParameters();
                psDel.setInt(1, coder_id);
                psDel.setInt(2, round_id);
                psDel.setInt(3, division_id);
                psDel.setInt(4, problem_id);
                psDel.executeUpdate();

                psIns.clearParameters();
                psIns.setInt(1, rs.getInt(1));  // coder_id
                psIns.setInt(2, rs.getInt(2));  // round_id
                psIns.setInt(3, rs.getInt(3));  // division_id
                psIns.setInt(4, rs.getInt(4));  // problem_id
                psIns.setFloat(5, rs.getFloat(5));  // submission_points
                psIns.setFloat(6, rs.getFloat(6));  // final_points
                psIns.setInt(7, rs.getInt(7));  // end_status_id
                psIns.setString(8, rs.getString(8));  // end_status_text
                psIns.setLong(9, rs.getLong(9));  // open_time
                psIns.setLong(10, rs.getLong(10));  // submit_time
                psIns.setLong(11, elapsed_time);  // time_elapsed
                psIns.setInt(12, rs.getInt(12));  // language_id
                psIns.setFloat(13, rs.getFloat(13));  // challenge_points
                psIns.setFloat(14, rs.getFloat(14));  // system_test_points
                psIns.setFloat(15, rs.getFloat(15));  // defense_points
                psIns.setInt(16, open_order);  // open_order
                psIns.setInt(17, submit_order);  // submit_order
                psIns.setInt(18, level_id);
                psIns.setString(19, level_desc);

                retVal = psIns.executeUpdate();
                count += retVal;
                if (retVal != 1) {
                    throw new SQLException("TCLoadRound: Insert for coder_id " +
                            coder_id + ", round_id " + round_id +
                            ", division_id " + division_id +
                            ", problem_id " + problem_id +
                            " modified " + retVal + " rows, not one.");
                }

                printLoadProgress(count, "coder_problem");
            }

            log.info("Coder_problem records copied = " + count);
        } catch (SQLException sqle) {
            DBMS.printSqlException(true, sqle);
            throw new Exception("Load of 'coder_problem' table failed.\n" +
                    "coder: " + coder_id + ", round_id " + round_id +
                    ", division_id " + division_id +
                    ", problem_id " + problem_id +
                    sqle.getMessage());
        } finally {
            close(rs);
            close(rs2);
            close(psSel);
            close(psSel2);
            close(psIns);
            close(psDel);
        }
    }

    /**
     * This populates the 'challenge' table
     */
    private void loadChallenge() throws Exception {
        int retVal = 0;
        int count = 0;
        PreparedStatement psSel = null;
        PreparedStatement psIns = null;
        PreparedStatement psDel = null;
        PreparedStatement psUpd = null;
        ResultSet rs = null;
        StringBuffer query = null;

        try {
            query = new StringBuffer(100);
            query.append("SELECT chal.challenge_id ");        // 1
            query.append("       ,chal.defendant_id ");       // 2
            query.append("       ,comp.problem_id ");         // 3
            query.append("       ,chal.round_id ");           // 4
            query.append("       ,chal.succeeded ");          // 5
            query.append("       ,chal.submit_time ");        // 6
            query.append("       ,chal.challenger_id ");      // 7
            query.append("       ,chal.args ");               // 8
            query.append("       ,chal.message ");            // 9
            query.append("       ,chal.challenger_points ");  // 10
            query.append("       ,chal.defendant_points ");   // 11
            query.append("       ,chal.expected ");           // 12
            query.append("       ,chal.received ");           // 13
            query.append("  FROM challenge chal, component comp");
            query.append(" WHERE chal.round_id = ? ");
            query.append("   AND chal.component_id = comp.component_id");
            query.append("   AND chal.status_id <> " + CHALLENGE_NULLIFIED);
            query.append("   AND EXISTS ");
            query.append("     (SELECT 'dok' ");
            query.append("        FROM user_group_xref ugx");
            query.append("       WHERE ugx.login_id = chal.defendant_id");
            query.append("         AND ugx.group_id = " + STUDENT_GROUP_ID + ")");

            psSel = prepareStatement(query.toString(), SOURCE_DB);

            query = new StringBuffer(100);
            query.append("INSERT INTO challenge ");
            query.append("      (challenge_id ");        // 1
            query.append("       ,defendant_id ");       // 2
            query.append("       ,problem_id ");         // 3
            query.append("       ,round_id ");           // 4
            query.append("       ,succeeded ");          // 5
            query.append("       ,submit_time ");        // 6
            query.append("       ,challenger_id ");      // 7
            query.append("       ,args ");               // 8
            query.append("       ,message ");            // 9
            query.append("       ,challenger_points ");  // 10
            query.append("       ,defendant_points ");   // 11
            query.append("       ,expected ");           // 12
            query.append("       ,received) ");          // 13
            query.append("VALUES (");
            query.append("?,?,?,?,?,?,?,?,?,?,");  // 10 values
            query.append("?,?,?)");                // 13 total values
            psIns = prepareStatement(query.toString(), TARGET_DB);

            query = new StringBuffer(100);
            query.append("DELETE FROM challenge ");
            query.append(" WHERE round_id = ? ");
            psDel = prepareStatement(query.toString(), TARGET_DB);

            // On to the load
            psSel.setInt(1, fRoundId);
            rs = psSel.executeQuery();

            // First thing we do is delete all the challenge entries for this round
            psDel.setInt(1, fRoundId);
            psDel.executeUpdate();

            while (rs.next()) {
                int challenger_id = rs.getInt(7);
                int defendant_id = rs.getInt(2);

                psIns.clearParameters();
                psIns.setInt(1, rs.getInt(1));  // challenge_id
                psIns.setInt(2, rs.getInt(2));  // defendant_id
                psIns.setInt(3, rs.getInt(3));  // problem_id
                psIns.setInt(4, rs.getInt(4));  // round_id
                psIns.setInt(5, rs.getInt(5));  // succeeded
                psIns.setLong(6, rs.getLong(6));  // submit_time
                psIns.setInt(7, rs.getInt(7));  // challenger_id
                setBytes(psIns, 8, getBlobObject(rs, 8));  // args
                psIns.setString(9, rs.getString(9));  // message
                psIns.setFloat(10, rs.getFloat(10));  // challenger_points
                psIns.setFloat(11, rs.getFloat(11));  // defendant_points
                setBytes(psIns, 12, getBlobObject(rs, 12));  // expected
                setBytes(psIns, 13, getBlobObject(rs, 13));  // received

                retVal = psIns.executeUpdate();
                count += retVal;
                if (retVal != 1) {
                    throw new SQLException("TCLoadRound: Insert for challenge_id " +
                            rs.getInt(1) +
                            " modified " + retVal + " rows, not one.");
                }

                printLoadProgress(count, "challenge");
            }

            log.info("Challenge records copied = " + count);
        } catch (SQLException sqle) {
            DBMS.printSqlException(true, sqle);
            throw new Exception("Load of 'challenge' table failed.\n" +
                    sqle.getMessage());
        } finally {
            close(rs);
            close(psSel);
            close(psIns);
            close(psDel);
        }
    }

    /**
     * This method places the start time of the load into the update_log table
     */
    private void setLastUpdateTime() throws Exception {
        PreparedStatement psUpd = null;
        StringBuffer query = null;

        try {
            int retVal = 0;
            query = new StringBuffer(100);
            query.append("INSERT INTO update_log ");
            query.append("      (log_id ");        // 1
            query.append("       ,calendar_id ");  // 2
            query.append("       ,timestamp  ");   // 3
            query.append("       ,log_type_id) ");   // 4
            query.append("VALUES (0, ?, ?, " + ROUND_LOG_TYPE + ")");
            psUpd = prepareStatement(query.toString(), TARGET_DB);

            int calendar_id = lookupCalendarId(fStartTime, TARGET_DB);
            psUpd.setInt(1, calendar_id);
            psUpd.setTimestamp(2, fStartTime);

            retVal = psUpd.executeUpdate();
            if (retVal != 1) {
                throw new SQLException("SetLastUpdateTime updated " + retVal +
                        " rows, not just one.");
            }
        } catch (SQLException sqle) {
            DBMS.printSqlException(true, sqle);
            throw new Exception("Failed to set last log time.\n" +
                    sqle.getMessage());
        } finally {
            close(psUpd);
        }
    }

    private java.sql.Date getRoundStart(int roundId)
            throws SQLException {
        Integer iRoundId = new Integer(roundId);
        StringBuffer query = null;
        if (fRoundStartHT.get(iRoundId) != null)
            return (java.sql.Date) fRoundStartHT.get(iRoundId);

        query = new StringBuffer(100);
        query.append("SELECT rs.start_time ");
        query.append("  FROM round_segment rs ");
        query.append(" WHERE rs.round_id = ? ");
        query.append("   AND rs.segment_id = " + CODING_SEGMENT_ID);
        PreparedStatement pSel = prepareStatement(query.toString(), SOURCE_DB);

        pSel.setInt(1, roundId);
        ResultSet rs = pSel.executeQuery();

        if (rs.next()) {
            java.sql.Date date = rs.getDate(1);
            fRoundStartHT.put(new Integer(roundId), date);
            return date;
        } else {
            throw new SQLException("Unable to determine start for " + roundId);
        }
    }
}
