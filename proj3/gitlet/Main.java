package gitlet;


import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Tracy
 */
public class Main {

    /** Current Working Directory. */
    static final File CWD = new File(".");

    /** Main metadata folder. */
    static final File GITLET_FOLDER = new File(".gitlet");


    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException {
        if (args.length == 0) {
            exit("Please enter a command.");
        }
        switch (args[0]) {
        case "init":
            init();
            break;
        case "log":
            log();
            break;
        case "global-log":
            globallog();
            break;
        case "find":
            find(args);
            break;
        case "add":
            add(args);
            break;
        case "rm":
            rm(args);
            break;
        case "status":
            status();
            break;
        case "commit":
            commit(args, null);
            break;
        case "checkout":
            checkout(args);
            break;
        case "branch":
            branch(args);
            break;
        case "rm-branch":
            rmbranch(args);
            break;
        case "reset":
            reset(args);
            break;
        case "merge":
            merge(args);
            break;
        default:
            exit("No command with that name exists.");
        }
        return;
    }

    /** init command. */
    public static void init() throws IOException {
        if (!GITLET_FOLDER.exists()) {
            GITLET_FOLDER.mkdir();
            File bRANCHES = Utils.join(GITLET_FOLDER, "branches");
            bRANCHES.mkdir();
            File cOMMITS = Utils.join(GITLET_FOLDER, "commits");
            cOMMITS.mkdir();
            HashMap<String, String> empty = new HashMap<String, String>();
            Commit initial = new Commit("initial commit",
                    "Wed Dec 31 16:00:00 1969 -0800", empty, null, null);
            Branch master = new Branch("master", null, initial);
            Branch head = new Branch("head", "master", initial);
            initial.save();
            master.save();
            head.save();
            File aDDITION = Utils.join(GITLET_FOLDER, "addition");
            aDDITION.mkdir();
            File rEMOVAL = Utils.join(GITLET_FOLDER, "removal");
            rEMOVAL.mkdir();
        } else {
            exit("A Gitlet version-control system "
                    + "already exists in the current directory.");
        }
    }

    /** log command. */
    public static void log() throws IOException {
        if (!GITLET_FOLDER.exists()) {
            System.out.println("===\n");
        } else {
            File log = Utils.join(GITLET_FOLDER, "log");
            log.createNewFile();

            File hEAD = Utils.join(GITLET_FOLDER, "branches", "head");
            Branch headBranch = Utils.readObject(hEAD, Branch.class);
            Commit head = headBranch.getCommit();

            String id = head.id();
            String timestamp = head.time();
            String message = head.message();
            String curCommit = "";
            if (head.parent2() != null) {
                String p1 = head.parent().substring(0, 7);
                String p2 = head.parent2().substring(0, 7);
                curCommit = String.format("===\ncommit %s\nMerge: %s %s\n"
                        + "Date: %s\n%s", id, p1, p2, timestamp, message);
            } else {
                curCommit = String.format("===\ncommit %s\n"
                        + "Date: %s\n%s", id, timestamp, message);
            }
            Utils.writeContents(log, curCommit);

            while (head.parent() != null) {
                File p = Utils.join(GITLET_FOLDER, "commits", head.parent());
                Commit parent = Utils.readObject(p, Commit.class);
                String pId = parent.id();
                String pTimestamp = parent.time();
                String pMessage = parent.message();
                String pCommit = "";
                if (parent.parent2() != null) {
                    String pp1 = parent.parent().substring(0, 7);
                    String pp2 = parent.parent2().substring(0, 7);
                    pCommit = String.format("===\ncommit %s\nMerge: %s %s\n"
                            + "Date: %s\n%s",
                            pId, pp1, pp2, pTimestamp, pMessage);
                } else {
                    pCommit = String.format("===\ncommit %s\nDate: %s\n%s",
                            pId, pTimestamp, pMessage);
                }
                curCommit = curCommit + "\n\n" + pCommit;
                Utils.writeContents(log, curCommit);
                head = parent;
            }
            String logs = Utils.readContentsAsString(log);
            System.out.println(logs);
        }
    }

    /** global-log command. */
    public static void globallog() {
        File global = Utils.join(GITLET_FOLDER, "global-log");
        String glog = Utils.readContentsAsString(global);
        System.out.println(glog);
    }

    /** find command.
     * @param args operands
     * */
    public static void find(String[] args) {
        if (args.length != 2) {
            exit(String.format("Incorrect number of arguments for %s",
                    args[0]));
        } else {
            String message = args[1];
            boolean exist = false;
            File commitfolder = Utils.join(GITLET_FOLDER, "commits");
            File[] commits = commitfolder.listFiles();
            for (File f: commits) {
                Commit com = Utils.readObject(f, Commit.class);
                if (com.message().equals(message)) {
                    System.out.println(com.id());
                    exist = true;
                }
            }
            if (!exist) {
                exit("Found no commit with that message.");
            }
        }
    }

    /** add command.
     * @param args operands
     * @throws IOException
     * */
    public static void add(String[] args) throws IOException {
        if (args.length == 2) {
            String filename = args[1];
            File rmFile = Utils.join(GITLET_FOLDER, "removal", filename);
            File cur = new File(filename);
            if (rmFile.exists()) {
                cur.createNewFile();
                Utils.writeContents(cur, Utils.readContentsAsString(rmFile));
                rmFile.delete();
            } else {
                if (!cur.exists()) {
                    exit("File does not exist.");
                }
                String current = Utils.readContentsAsString(cur);
                File thisFile = Utils.join(GITLET_FOLDER, "addition", filename);

                File hEAD = Utils.join(GITLET_FOLDER, "branches", "head");
                Branch headBranch = Utils.readObject(hEAD, Branch.class);
                Commit head = headBranch.getCommit();
                if (head.files() != null
                        && head.files().containsKey(filename)) {
                    String com = head.files().get(filename);
                    if (com.equals(current)) {
                        if (thisFile.exists()) {
                            thisFile.delete();
                        }
                    } else {
                        thisFile.createNewFile();
                        Utils.writeContents(thisFile, current);
                    }
                } else {
                    thisFile.createNewFile();
                    Utils.writeContents(thisFile, current);
                }
            }
        } else {
            exit(String.format("Invalid number of arguments for %s", args[0]));
        }
    }

    /** rm command.
     * @param args operands
     * @throws IOException
     */
    public static void rm(String[] args) throws IOException {
        if (args.length == 2) {
            String filename = args[1];
            File sTAGEDADD = Utils.join(GITLET_FOLDER, "addition");
            File thisFile = Utils.join(sTAGEDADD, filename);

            File hEAD = Utils.join(GITLET_FOLDER, "branches", "head");
            Branch headBranch = Utils.readObject(hEAD, Branch.class);
            Commit head = headBranch.getCommit();
            boolean remove = true;

            if (thisFile.exists()) {
                thisFile.delete();
                remove = false;
            }
            if (head.files() != null && head.files().containsKey(filename)) {
                File file = new File(filename);
                File sTAGEDRM = Utils.join(GITLET_FOLDER, "removal");
                File deleteFile = Utils.join(sTAGEDRM, filename);
                deleteFile.createNewFile();
                if (file.exists()) {
                    String contents = Utils.readContentsAsString(file);
                    Utils.writeContents(deleteFile, contents);
                }
                file.delete();
                remove = false;
            }
            if (remove) {
                exit("No reason to remove the file");
            }
        }
    }

    /** status command. */
    public static void status() {
        if (!GITLET_FOLDER.exists()) {
            exit("Not in an initialized Gitlet directory.");
        }
        System.out.println("=== Branches ===");
        File[] branches = Utils.join(GITLET_FOLDER, "branches").listFiles();
        File hEAD = Utils.join(GITLET_FOLDER, "branches", "head");
        Branch headBranch = Utils.readObject(hEAD, Branch.class);
        Commit head = headBranch.getCommit();
        String current = headBranch.getHead();
        Arrays.sort(branches);
        for (File b: branches) {
            if (b.getName().equals(current)) {
                System.out.println("*" + b.getName());
            } else if (!b.getName().equals("head")) {
                System.out.println(b.getName());
            }
        }
        System.out.println("");
        System.out.println("=== Staged Files ===");
        File[] addition = Utils.join(GITLET_FOLDER,
                "addition").listFiles();
        if (addition.length > 0) {
            Arrays.sort(addition);
            for (File a : addition) {
                System.out.println(a.getName());
            }
        }
        System.out.println("");
        System.out.println("=== Removed Files ===");
        File[] removal = Utils.join(GITLET_FOLDER,
                "removal").listFiles();
        if (removal.length > 0) {
            Arrays.sort(removal);
            for (File r : removal) {
                System.out.println(r.getName());
            }
        }
        File[] working = CWD.listFiles();
        if (working.length > 0) {
            Arrays.sort(working);
        }
        System.out.println("");
        statusMod();
        System.out.println("=== Untracked Files ===");
        for (File f: working) {
            File inAdd = Utils.join(GITLET_FOLDER, "addition", f.getName());
            if (!head.files().containsKey(f.getName()) && !inAdd.exists()) {
                if (!f.getName().equals(".DS_Store")
                        && !f.getName().equals(".gitlet")
                        && !f.getName().equals("gitlet")) {
                    System.out.println(f.getName());
                }
            }
        }
        System.out.println("");
    }

    /** mod portion of status. */
    public static void statusMod() {
        File hEAD = Utils.join(GITLET_FOLDER, "branches", "head");
        Branch headBranch = Utils.readObject(hEAD, Branch.class);
        Commit head = headBranch.getCommit();
        File[] working = CWD.listFiles();
        if (working.length > 0) {
            Arrays.sort(working);
        }
        System.out.println("=== Modifications Not Staged For Commit ===");
        for (File f: working) {
            if (!f.getName().equals(".DS_Store")
                    && !f.getName().equals(".gitlet")
                    && !f.getName().equals("gitlet")) {
                String contents = Utils.readContentsAsString(f);
                String name = f.getName();
                File inAdd = Utils.join(GITLET_FOLDER, "addition", f.getName());
                if (head.files().containsKey(name)
                        && !head.files().get(name).equals(contents)) {
                    System.out.println(name + " (modified)");
                }
            }
        }
        for (Map.Entry<String, String> entry : head.files().entrySet()) {
            String k = entry.getKey();
            File f = new File(k);
            File inAdd = Utils.join(GITLET_FOLDER, "addition", f.getName());
            File inRm = Utils.join(GITLET_FOLDER, "removal", f.getName());
            if (!f.exists() && !inAdd.exists() && !inRm.exists()) {
                System.out.println(k + " (deleted)");
            }
        }
        System.out.println("");
    }

    /** commit command.
     * @param args operands
     * @param p2 parent 2
     * @throws IOException
     */
    public static void commit(String[] args, String p2) throws IOException {
        if (args.length == 2 && args[1].length() > 0) {
            String message = args[1];
            SimpleDateFormat datePattern =
                    new SimpleDateFormat("E MMM dd HH:mm:ss yyyy Z");
            Date date = new Date();
            String time = datePattern.format(date);

            boolean isMerge = false;
            if (p2 != null) {
                isMerge = true;
            }

            File sTAGEDA = Utils.join(GITLET_FOLDER, "addition");
            File[] stagedFilesA = sTAGEDA.listFiles();
            File sTAGEDR = Utils.join(GITLET_FOLDER, "removal");
            File[] stagedFilesR = sTAGEDR.listFiles();
            if (stagedFilesA.length == 0 && stagedFilesR.length == 0) {
                exit("No changes added to the commit.");
            }
            HashMap<String, String> curfiles = new HashMap<String, String>();
            File hEAD = Utils.join(GITLET_FOLDER, "branches", "head");
            Branch headBranch = Utils.readObject(hEAD, Branch.class);
            Commit head = headBranch.getCommit();
            curfiles = head.files();
            if (stagedFilesA.length != 0) {
                for (File f : stagedFilesA) {
                    curfiles.put(f.getName(), Utils.readContentsAsString(f));
                    f.delete();
                }
            }
            if (stagedFilesR.length != 0) {
                for (File f : stagedFilesR) {
                    curfiles.remove(f.getName());
                    f.delete();
                }
            }

            Commit commit = new Commit(message, time,
                    curfiles, head.id(), null);
            if (isMerge) {
                commit.setParent2(p2);
            }
            commit.save();
            headBranch.setCommit(commit);
            Utils.writeObject(hEAD, headBranch);
            File cur = Utils.join(GITLET_FOLDER,
                    "branches", headBranch.getHead());
            Utils.writeObject(cur, headBranch);
        } else {
            exit("Please enter a commit message.");
        }
    }

    /** checkout command.
     * @param args operands
     * @throws IOException
     */
    public static void checkout(String[] args) throws IOException {
        File hEAD = Utils.join(GITLET_FOLDER, "branches", "head");
        Branch headBranch = Utils.readObject(hEAD, Branch.class);
        Commit head = headBranch.getCommit();
        if (args.length <= 1) {
            exit(String.format("Incorrect number of arguments for %s",
                    args[0]));
        } else {
            if (args.length == 3 && args[1].equals("--")) {
                String filename = args[2];
                boolean inCommit = head.files().containsKey(filename);
                if (inCommit) {
                    File file = new File(filename);
                    String contents = head.files().get(filename);
                    file.createNewFile();
                    Utils.writeContents(file, contents);
                } else {
                    exit("File does not exist in that commit.");
                }
            } else if (args.length == 4) {
                String filename = args[3];
                File commits = Utils.join(GITLET_FOLDER, "commits");
                String commitname = "";
                for (File c: commits.listFiles()) {
                    if (c.getName().contains(args[1])) {
                        commitname = c.getName();
                    }
                }
                if (!args[2].equals("--")) {
                    exit("Incorrect operands.");
                }
                File commit = Utils.join(GITLET_FOLDER, "commits", commitname);
                if (!commit.getName().equals("commits")) {
                    Commit com = Utils.readObject(commit, Commit.class);
                    if (com.files().containsKey(filename)) {
                        File file = new File(filename);
                        String contents = com.files().get(filename);
                        file.createNewFile();
                        Utils.writeContents(file, contents);
                    } else {
                        exit("File does not exist in that commit.");
                    }
                } else {
                    exit("No commit with that id exists.");
                }
            } else if (args.length == 2) {
                checkoutHelper(args[1]);

            } else {
                exit("Invalid checkout operand");
            }
        }
    }

    /** checkout helper for usage3.
     * @param branchName branch name
     */
    public static void checkoutHelper(String branchName) {
        File hEAD = Utils.join(GITLET_FOLDER, "branches", "head");
        Branch headBranch = Utils.readObject(hEAD, Branch.class);
        Commit head = headBranch.getCommit();
        File bRANCH = Utils.join(GITLET_FOLDER, "branches", branchName);
        if (bRANCH.exists()) {
            if (!headBranch.getHead().equals(branchName)) {
                Branch branch = Utils.readObject(bRANCH, Branch.class);
                Commit commit = branch.getCommit();
                HashMap<String, String> files = commit.files();
                for (Map.Entry<String, String> entry : files.entrySet()) {
                    String k = entry.getKey();
                    File f = new File(k);
                    if (f.exists()) {
                        String contents = Utils.readContentsAsString(f);
                        if (!head.files().containsKey(k)
                                || !contents.equals(head.files().get(k))) {
                            exit("There is an untracked file in the way; "
                                    + "delete it, or add and commit it first.");
                        }
                    }
                    String v = entry.getValue();
                    Utils.writeContents(f, v);
                }
                for (File f : CWD.listFiles()) {
                    String name = f.getName();
                    if (head.files().containsKey(name)
                            && !files.containsKey(name)) {
                        File cwdfile = new File(name);
                        cwdfile.delete();
                    }
                }
                headBranch.setHead(branchName);
                headBranch.setCommit(commit);
                Utils.writeObject(hEAD, headBranch);
                clearStage();
            } else {
                exit("No need to checkout the current branch.");
            }
        } else {
            exit("No such branch exists.");
        }
    }

    /** branch command.
     * @param args operands
     * @throws IOException
     */
    public static void branch(String[] args) throws IOException {
        if (args.length != 2) {
            exit(String.format("Incorrect number of arguments for %s",
                    args[0]));
        } else {
            String name = args[1];
            File newBranch = Utils.join(GITLET_FOLDER, "branches", name);
            if (!newBranch.exists()) {
                File hEAD = Utils.join(GITLET_FOLDER, "branches", "head");
                Branch headBranch = Utils.readObject(hEAD, Branch.class);
                Commit head = headBranch.getCommit();
                Branch branch = new Branch(name, null, head);
                newBranch.createNewFile();
                Utils.writeObject(newBranch, branch);
            } else {
                exit("A branch with that name already exists.");
            }
        }
    }

    /** rm-branch command.
     * @param args operands
     * @throws IOException
     */
    public static void rmbranch(String[] args) throws IOException {
        if (args.length != 2) {
            exit(String.format("Incorrect number of arguments for %s",
                    args[0]));
        } else {
            String name = args[1];
            File branch = Utils.join(GITLET_FOLDER, "branches", name);
            if (branch.exists()) {
                File hEAD = Utils.join(GITLET_FOLDER, "branches", "head");
                Branch headBranch = Utils.readObject(hEAD, Branch.class);
                if (headBranch.getHead().equals(name)) {
                    exit("Cannot remove the current branch.");
                } else {
                    branch.delete();
                }
            } else {
                exit("A branch with that name does not exist.");
            }
        }
    }

    /** reset command.
     * @param args operands
     * @throws IOException
     */
    public static void reset(String[] args) throws IOException {
        if (args.length != 2) {
            exit(String.format("Incorrect number of arguments for %s",
                    args[0]));
        } else {
            File hEAD = Utils.join(GITLET_FOLDER, "branches", "head");
            Branch headBranch = Utils.readObject(hEAD, Branch.class);
            Commit head = headBranch.getCommit();
            File commits = Utils.join(GITLET_FOLDER, "commits");
            String commitname = "";
            for (File c: commits.listFiles()) {
                if (c.getName().contains(args[1])) {
                    commitname = c.getName();
                }
            }
            File com = Utils.join(GITLET_FOLDER, "commits", commitname);
            if (!com.getName().equals("commits")) {
                Commit commit = Utils.readObject(com, Commit.class);
                HashMap<String, String> files = commit.files();
                for (Map.Entry<String, String> entry : files.entrySet()) {
                    String k = entry.getKey();
                    File f = new File(k);
                    if (f.exists()) {
                        String contents = Utils.readContentsAsString(f);
                        if (!head.files().containsKey(k)
                                || !contents.equals(head.files().get(k))) {
                            exit("There is an untracked file in the way; "
                                    + "delete it, or add and commit it first.");
                        }
                    }
                    String v = entry.getValue();
                    Utils.writeContents(f, v);
                }
                for (File f : CWD.listFiles()) {
                    String name = f.getName();
                    if (head.files().containsKey(name)
                            && !files.containsKey(name)) {
                        File cwdfile = new File(name);
                        cwdfile.delete();
                    }
                }
                headBranch.setCommit(commit);
                Utils.writeObject(hEAD, headBranch);
                clearStage();
                String thebranch = headBranch.getHead();
                File branchFile = Utils.join(GITLET_FOLDER,
                        "branches", thebranch);
                Branch branch = Utils.readObject(branchFile, Branch.class);
                branch.setCommit(commit);
                Utils.writeObject(branchFile, branch);
            } else {
                exit("No commit with that id exists.");
            }
        }
    }

    /** merge command.
     * @param args operands
     * @throws IOException
     */
    public static void merge(String[] args) throws IOException {
        File bRANCH = Utils.join(GITLET_FOLDER, "branches", args[1]);
        File hEAD = Utils.join(GITLET_FOLDER, "branches", "head");
        Branch headBranch = Utils.readObject(hEAD, Branch.class);
        if (!bRANCH.exists()) {
            exit("A branch with that name does not exist.");
        }
        Branch givenBranch = Utils.readObject(bRANCH, Branch.class);
        Commit head = headBranch.getCommit();
        Commit given = givenBranch.getCommit();
        Commit split = split(args[1]);
        mergeCheckStaged();
        mergeCheckUntracked(given, head);
        mergeCheckSplit(split, given, head, args[1], headBranch);
        HashMap<String, String> givenFiles = given.files();
        for (Map.Entry<String, String> entry : givenFiles.entrySet()) {
            String k = entry.getKey();
            String givenV = entry.getValue();
            String splitV = split.files().get(k);
            String curV = head.files().get(k);
            if (splitV != null && curV != null) {
                if (!givenV.equals(splitV) && splitV.equals(curV)) {
                    checkout(new String[]{"checkout", given.id(), "--", k});
                    add(new String[]{"add", k});
                } else if (!givenV.equals(splitV) && !curV.equals(splitV)
                        && !givenV.equals(curV)) {
                    mergeConflict(curV, givenV, k);
                }
            } else if (splitV == null) {
                if (curV == null) {
                    checkout(new String[]{"checkout",
                            given.id(), "--", k});
                    add(new String[]{"add", k});
                } else if (!givenV.equals(curV)) {
                    mergeConflict(curV, givenV, k);
                }
            }
        }
        for (Map.Entry<String, String> entry : split.files().entrySet()) {
            String k = entry.getKey();
            String splitV = entry.getValue();
            String givenV = given.files().get(k);
            String curV = head.files().get(k);
            if (curV != null && splitV != null) {
                if (curV.equals(splitV) && givenV == null) {
                    rm(new String[]{"rm", k});
                } else if (givenV == null && !curV.equals(splitV)) {
                    mergeConflict(curV, "", k);
                }
            } else if (curV == null && givenV != null
                    && !givenV.equals(splitV)) {
                mergeConflict("", givenV, k);
            }
        }
        String mergeMSG = "Merged " + args[1]
                + " into " + headBranch.getHead() + ".";
        commit(new String[]{"commit", mergeMSG}, given.id());
    }

    /** split helper.
     * @param branch branch name
     * @return
     */
    public static Commit split(String branch) {
        File hEAD = Utils.join(GITLET_FOLDER, "branches", "head");
        Branch headBranch = Utils.readObject(hEAD, Branch.class);
        Commit head = headBranch.getCommit();
        File bRANCH = Utils.join(GITLET_FOLDER, "branches", branch);
        Branch br = Utils.readObject(bRANCH, Branch.class);
        while (head.parent() != null) {
            if (head.parent2() != null) {
                File p = Utils.join(GITLET_FOLDER, "commits", head.parent2());
                Commit head2 = Utils.readObject(p, Commit.class);
                while (head2.parent() != null) {
                    Commit com = br.getCommit();
                    File p2 = Utils.join(GITLET_FOLDER,
                            "commits", head.parent());
                    Commit parent = Utils.readObject(p2, Commit.class);
                    if (head2.parent().equals(com.id())) {
                        return com;
                    }
                    while (com.parent() != null) {
                        if (head2.id().equals(com.parent())) {
                            return head2;
                        }
                        if (head2.parent().equals(com.parent())) {
                            return parent;
                        }
                        File pc = Utils.join(GITLET_FOLDER,
                                "commits", com.parent());
                        Commit parentCom = Utils.readObject(pc, Commit.class);
                        com = parentCom;
                    }
                }
            }
            Commit com = br.getCommit();
            File p = Utils.join(GITLET_FOLDER, "commits", head.parent());
            Commit parent = Utils.readObject(p, Commit.class);
            if (head.parent().equals(com.id())) {
                return com;
            }
            while (com.parent() != null) {
                if (head.id().equals(com.parent())) {
                    return head;
                }
                if (head.parent().equals(com.parent())) {
                    return parent;
                }
                File pc = Utils.join(GITLET_FOLDER, "commits", com.parent());
                Commit parentCom = Utils.readObject(pc, Commit.class);
                com = parentCom;
            }
            head = parent;
        }
        return null;
    }

    /** split helper. */
    public static void mergeCheckStaged() {
        File sTAGEDA = Utils.join(GITLET_FOLDER, "addition");
        File sTAGEDR = Utils.join(GITLET_FOLDER, "removal");
        if (sTAGEDA.listFiles().length > 0
                || sTAGEDR.listFiles().length > 0) {
            exit("You have uncommitted changes.");
        }
    }

    /** split helper.
     * @param given given commit
     * @param head current commit
     */
    public static void mergeCheckUntracked(Commit given, Commit head) {
        for (Map.Entry<String, String> entry
                : given.files().entrySet()) {
            String k = entry.getKey();
            File f = new File(k);
            if (f.exists()) {
                String contents = Utils.readContentsAsString(f);
                if (!head.files().containsKey(k)
                        || !contents.equals(head.files().get(k))) {
                    exit("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                }
            }
        }
    }

    /** split helper.
     * @param split split commit
     * @param given given commit
     * @param head current commit
     * @param branchName branch name
     * @param headBranch current branch
     * @throws IOException
     */
    public static void mergeCheckSplit(Commit split, Commit given,
                                       Commit head, String branchName,
                                       Branch headBranch) throws IOException {
        if (split.id().equals(given.id())) {
            exit("Given branch is an ancestor of the current branch.");
        } else if (split.id().equals(head.id())) {
            String[] op = new String[] {"checkout", branchName};
            checkout(op);
            exit("Current branch fast-forwarded.");
        } else if (headBranch.getHead().equals(branchName)) {
            exit("Cannot merge a branch with itself.");
        }
    }

    /** split helper.
     * @param curV value of current file
     * @param givenV value of given file
     * @param k file name
     * @return
     */
    public static void mergeConflict(String curV,
                                     String givenV,
                                     String k) throws IOException {
        System.out.println("Encountered a merge conflict.");
        String msg1 = "<<<<<<< HEAD\n";
        String msg2 = curV;
        String msg3 = "=======\n";
        String msg4 = givenV;
        String msg5 = ">>>>>>>\n";
        String msg = msg1 + msg2 + msg3 + msg4 + msg5;
        Utils.writeContents(new File(k), msg);
        add(new String[]{"add", k});
    }

    /** clear staging area helper. */
    public static void clearStage() {
        File add = Utils.join(GITLET_FOLDER, "addition");
        File rm = Utils.join(GITLET_FOLDER, "removal");
        File[] addfiles = add.listFiles();
        File[] rmfiles = rm.listFiles();
        for (File f : addfiles) {
            f.delete();
        }
        for (File f : rmfiles) {
            f.delete();
        }
    }

    /** exit error.
     * @param message exit message
     */
    public static void exit(String message) {
        System.out.println(message);
        System.exit(0);
    }
}

