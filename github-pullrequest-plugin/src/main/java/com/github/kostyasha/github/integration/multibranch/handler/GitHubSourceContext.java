package com.github.kostyasha.github.integration.multibranch.handler;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.action.GitHubRepo;
import com.github.kostyasha.github.integration.multibranch.head.GitHubSCMHead;
import com.github.kostyasha.github.integration.multibranch.revision.GitHubSCMRevision;

import hudson.model.Job;
import hudson.model.TaskListener;
import jenkins.branch.BranchProjectFactory;
import jenkins.branch.MultiBranchProject;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSourceCriteria;
import jenkins.scm.api.SCMSourceOwner;

public class GitHubSourceContext {

    private final GitHubSCMSource source;
    private final SCMHeadObserver observer;
    private final SCMSourceCriteria criteria;
    private final SCMHeadEvent<?> scmHeadEvent;
    private final GitHubRepo localRepo;
    private final GHRepository remoteRepo;
    private final TaskListener listener;

    public GitHubSourceContext( //
            @Nonnull GitHubSCMSource source, //
            @Nonnull SCMHeadObserver observer, //
            @Nonnull SCMSourceCriteria criteria, //
            @Nullable SCMHeadEvent<?> scmHeadEvent, //
            @Nonnull GitHubRepo localRepo, //
            @Nonnull GHRepository remoteRepo, //
            @Nonnull TaskListener listener) {
        this.source = source;
        this.observer = observer;
        this.criteria = criteria;
        this.scmHeadEvent = scmHeadEvent;
        this.localRepo = localRepo;
        this.remoteRepo = remoteRepo;
        this.listener = listener;
    }

    public @Nonnull GitHubSCMSource getSource() {
        return source;
    }

    public @Nonnull SCMHeadObserver getObserver() {
        return observer;
    }

    public SCMHeadEvent<?> getScmHeadEvent() {
        return scmHeadEvent;
    }

    public @Nonnull GitHubRepo getLocalRepo() {
        return localRepo;
    }

    public @Nonnull GHRepository getRemoteRepo() {
        return remoteRepo;
    }

    public @Nonnull TaskListener getListener() {
        return listener;
    }

    public GitHub getGitHub() {
        return source.getRepoProvider().getGitHub(source);
    }

    public boolean checkCriteria(@Nonnull GitHubSCMHead head, @Nonnull GitHubSCMRevision revision) throws IOException {
        if (criteria != null) {
            listener.getLogger().println("Checking " + head.getPronoun());
            if (!criteria.isHead(source.newProbe(head, revision), listener)) {
                listener.getLogger().println("  Didn't meet criteria\n");
                return false;
            }
            listener.getLogger().println("  Met criteria\n");
        }
        return true;
    }

    /**
     * Force job to unconditionally build on any next revision
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void forceNextBuild(SCMRevision nextRev) throws IOException {
        SCMSourceOwner owner = source.getOwner();
        if (owner instanceof MultiBranchProject) {
            MultiBranchProject mb = (MultiBranchProject) owner;

            BranchProjectFactory pf = mb.getProjectFactory();
            for (Object o : mb.getItems()) {
                Job j = (Job) o;
                SCMRevision rev = pf.getRevision(j);
                if (rev != null && rev.equals(nextRev)) {
                    pf.setRevisionHash(j, new DummyRevision(nextRev.getHead()));
                }
            }
        }
    }

    /**
     * Special revision to unconditionally force next build
     */
    public static class DummyRevision extends SCMRevision {
        private static final long serialVersionUID = 1L;

        public DummyRevision(SCMHead head) {
            super(head);
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            return false;
        }
    };
}
