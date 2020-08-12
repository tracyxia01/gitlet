package gitlet;

import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/** Commit class with Commit constructor.
 * @author Tracy
 */
public class Commit implements Serializable {

    /** File referring to the commits folder in .gitlet. */
    static final File COMMIT_FOLDER = new File(".gitlet/commits");
    /** File referring to the global-log file in .gitlet. */
    static final File GLOBAL_LOG = Utils.join(".gitlet", "global-log");

    /** String to store message. */
    private String _message;
    /** String to store timestamp. */
    private String _timestamp;
    /** HashMap to store files. */
    private HashMap<String, String> _files;
    /** String to store id. */
    private String _id;
    /** String to store parent. */
    private String _parent;
    /** String to store second parent. */
    private String _parent2;

    /** Commit constructor.
     * @param f files
     * @param m message
     * @param p parent
     * @param p2 parent2
     * @param t timestamp
     * */
    public Commit(String m, String t, HashMap<String,
            String> f, String p, String p2) {
        _message = m;
        _timestamp = t;
        _files = f;
        _parent = p;
        _parent2 = p2;
        byte[] c = Utils.serialize(this);
        _id = Utils.sha1(c);
    }

    /** Saves this commit to commits folder. */
    public void save() throws IOException {
        File commit = Utils.join(COMMIT_FOLDER, this._id);
        commit.createNewFile();
        Utils.writeObject(commit, this);
        GLOBAL_LOG.createNewFile();

        String old = Utils.readContentsAsString(GLOBAL_LOG);
        String curCommit = String.format("===\ncommit %s\nDate: %s\n%s",
                _id, _timestamp, _message);
        if (this._message.equals("initial commit")) {
            Utils.writeContents(GLOBAL_LOG, curCommit);
        } else {
            String total = curCommit + "\n\n" + old;
            Utils.writeContents(GLOBAL_LOG, total);
        }
    }

    /** returns commit id. */
    public String id() {
        return _id;
    }

    /** returns time. */
    public String time() {
        return _timestamp;
    }

    /** returns message. */
    public String message() {
        return _message;
    }

    /** returns parent. */
    public String parent() {
        return _parent;
    }

    /** returns any second parent. */
    public String parent2() {
        return _parent2;
    }

    /** returns files stored. */
    public HashMap<String, String> files() {
        return _files;
    }

    /** sets files of commits to new set of files.
     * @param files files
     * */
    public void setFiles(HashMap<String, String> files) {
        _files = files;
    }

    /** sets parent2.
     * @param p2 parent2
     * */
    public void setParent2(String p2) {
        _parent2 = p2;
    }
}
