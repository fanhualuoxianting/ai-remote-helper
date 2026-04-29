package com.airh.agent.ui;

import com.airh.agent.safety.PathSandbox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

public class AuthorizedDirectoryChooser {
    public Optional<Selection> choose(Window owner) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择授权工作目录");
        File directory = chooser.showDialog(owner);
        if (directory == null) {
            return Optional.empty();
        }

        Path authorizedDirectory = directory.toPath().toAbsolutePath().normalize();
        return Optional.of(new Selection(authorizedDirectory, new PathSandbox(authorizedDirectory)));
    }

    public record Selection(Path authorizedDirectory, PathSandbox sandbox) {
    }
}
