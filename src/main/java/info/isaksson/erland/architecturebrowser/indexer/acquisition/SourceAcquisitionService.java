package info.isaksson.erland.architecturebrowser.indexer.acquisition;

public final class SourceAcquisitionService {
    private final LocalPathSourceAcquirer localPathSourceAcquirer;
    private final GitSourceAcquirer gitSourceAcquirer;

    public SourceAcquisitionService() {
        this(new LocalPathSourceAcquirer(), new GitSourceAcquirer());
    }

    SourceAcquisitionService(LocalPathSourceAcquirer localPathSourceAcquirer, GitSourceAcquirer gitSourceAcquirer) {
        this.localPathSourceAcquirer = localPathSourceAcquirer;
        this.gitSourceAcquirer = gitSourceAcquirer;
    }

    public AcquisitionResult acquire(AcquisitionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (request.isLocalPathRequest() == request.isGitRequest()) {
            throw new IllegalArgumentException("Exactly one of localPath or gitUrl must be provided");
        }
        if (request.isLocalPathRequest()) {
            return localPathSourceAcquirer.acquire(request);
        }
        return gitSourceAcquirer.acquire(request);
    }
}
