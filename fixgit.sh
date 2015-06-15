git filter-branch --force --commit-filter '
        cat "$GIT_COMMITTER_EMAIL" 
        if [ "$GIT_COMMITTER_EMAIL" = "<ben.pan.nj@gmail.com" ];
        then
                GIT_COMMITTER_EMAIL="ben.pan.nj@gmail.com";
                GIT_AUTHOR_EMAIL="ben.pan.nj@gmail.com";
                git commit-tree "$@";
        else
                GIT_COMMITTER_EMAIL="ben.pan.nj@gmail.com";
                GIT_AUTHOR_EMAIL="ben.pan.nj@gmail.com";
                git commit-tree "$@";
        fi' refs/original/refs/heads/master -- --all
