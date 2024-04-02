import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class ResumableGitClone {

    private static final String DEPTH_RECORD_FILE = ".resumable_git_depth";
    private static final String OUT_FILE = "rgit.out";
    private static final String GIT_COMMAND = "git";
    private static final String CLONE_COMMAND = "clone";
    private static final String FETCH_COMMAND = "fetch";
    private static final String ORIGIN_REMOTE = "origin";
    private final String repositoryUrl;

    public ResumableGitClone(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public void cloneRepository(int depth, int step) {
        try {
            Process process = executeCommand(GIT_COMMAND, CLONE_COMMAND, "--depth", String.valueOf(depth), repositoryUrl);
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("Repository cloned successfully");
                fetchHistory(depth, step);
            } else {
                System.out.println("Error: Failed to clone the repository");
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Error:" + e.getMessage());
        }
    }

    private void fetchHistory(int depth, int step) {
        int currentDepth = depth;
        while (true) {
            try {
                Process process = executeCommand(GIT_COMMAND, FETCH_COMMAND, "--depth", String.valueOf(currentDepth), ORIGIN_REMOTE);
                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    if (fetchComplete(process)) {
                        System.out.println("Fetch completed");
                        break;
                    } else {
                        currentDepth += step;
                    }
                } else {
                    currentDepth -= step / 2;
                    if (currentDepth <= 0) {
                        System.out.println("Error: Failed to fetch the repository history");
                        break;
                    }
                }
            } catch (IOException | InterruptedException e) {
                System.out.println("Error: " + e.getMessage());
                break;
            }
        }

        fetchBranches();
    }

    private boolean fetchComplete(Process process) throws IOException {
        return process.exitValue() == 0;
    }

    private void fetchBranches() {
    try {
        Process process = executeCommand(GIT_COMMAND, FETCH_COMMAND, "--all");
        int exitCode = process.waitFor();
        if (exitCode == 0) {
            System.out.println("Branches fetched successfully");
        } else {
            System.out.println("Error: Failed to fetch the branches");
        }
    } catch (IOException | InterruptedException e) {
        System.out.println("Error: " + e.getMessage());
    }
}


    private Process executeCommand(String... command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File("."));
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        return process;
    }

    public static void main(String[] args) {
    if (args.length != 2) {
        System.out.println("Usage: java ResumableGitClone <repositoryUrl> <depth>");
        return;
    }

    String repositoryUrl = args[0];
    int depth = Integer.parseInt(args[1]);
    int step = 2; 

    ResumableGitClone clone = new ResumableGitClone(repositoryUrl);
    clone.cloneRepository(depth, step);
}

}
