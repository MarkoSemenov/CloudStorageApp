package ru.geekbrains.storage_demo_app.controller;

import org.primefaces.PrimeFaces;
import org.primefaces.event.*;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.TreeNode;
import org.primefaces.model.file.UploadedFile;
import org.primefaces.model.file.UploadedFiles;
import ru.geekbrains.storage_demo_app.entities.DownloadFile;
import ru.geekbrains.storage_demo_app.entities.File;
import ru.geekbrains.storage_demo_app.entities.Folder;
import ru.geekbrains.storage_demo_app.service.FileProcess;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

@Named
@SessionScoped
public class IndexController implements Serializable {

    @EJB
    private FileProcess fileProcess;

    private UploadedFile file;
    private UploadedFiles files;
    private List<File> userFiles;
    private File selectedFile;
    private StreamedContent downloadFile;
    private TreeNode root;
    private TreeNode selectedNode;
    private Folder selectedFolder;
    private String newFolderName;
    private Folder rootFolder;
    private String menuItemId;

    @Inject
    private HttpSession httpSession;


    @PostConstruct
    public void init() {

        rootFolder = fileProcess.getRootUserFolder();
        userFiles = fileProcess.getFiles();
        root = fileProcess.getTreeViewRoot();

    }


    public String logout() {
        httpSession.invalidate();
        return "/authentication.xhtml";
    }


    public void deleteSelectedFile() {
        if (selectedFile == null) {
            FacesMessage message = new FacesMessage("File", "File isn't selected");
            FacesContext.getCurrentInstance().addMessage(null, message);
        } else {
            fileProcess.deleteFile(selectedFile.getId());
            selectedFile = null;
            downloadFile = null;
            PrimeFaces.current().ajax().update("files_table:eventsDT");
            updateFilesView();
        }
    }


    public void createFolder() {
        Folder folder = new Folder(newFolderName, "Folder");
        folder.setParent(selectedFolder != null ? selectedFolder : rootFolder);
        fileProcess.createFolder(folder);
        new DefaultTreeNode(folder, selectedNode != null ? selectedNode : root);
        PrimeFaces.current().executeScript("PF('manageFolderDialog').hide()");
        PrimeFaces.current().ajax().update("form:tree");
        newFolderName = null;
    }


    public File findFile(Long id) {
        return fileProcess.findFileById(id);
    }

    public StreamedContent getFileDownload() {
        if (selectedFile == null) {
            FacesMessage message = new FacesMessage("File", "File isn't selected");
            FacesContext.getCurrentInstance().addMessage(null, message);
        } else {
            downloadFile = new DownloadFile(findFile(selectedFile.getId()));
        }
        return downloadFile;
    }


    public void handleFileUpload(FileUploadEvent event) throws IOException {
        UploadedFile uploadedFile = event.getFile();
        File file = new File(uploadedFile.getFileName(), uploadedFile.getContentType(), uploadedFile.getSize(), uploadedFile.getContent());
        file.setFolder(selectedFolder != null ? selectedFolder : rootFolder);
        fileProcess.upload(file);
        FacesMessage message = new FacesMessage("Successful", event.getFile().getFileName() + " is uploaded.");
        FacesContext.getCurrentInstance().addMessage(null, message);
        PrimeFaces.current().ajax().update("files_table:eventsDT");
        updateFilesView();
        try {
            uploadedFile.getInputStream().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteSelectedFolder() {
        if (selectedFolder == null) {
            FacesMessage message = new FacesMessage("Folder", "Folder is not selected");
            FacesContext.getCurrentInstance().addMessage(null, message);
        } else {
            fileProcess.deleteFolder(selectedFolder);
            root.getChildren().remove(selectedNode);
            removeNode(root.getChildren(), selectedNode);
            FacesMessage message = new FacesMessage("Successful", selectedFolder.getName() + " is deleted.");
            FacesContext.getCurrentInstance().addMessage(null, message);
            selectedFolder = null;
            selectedNode = null;
            PrimeFaces.current().ajax().update("form:tree");
            PrimeFaces.current().ajax().update("files_table:eventsDT");
            updateFilesView();
        }
    }


    public void removeNode(List<TreeNode> nodes, TreeNode node) {
        for (TreeNode tr : nodes) {
            if (tr.getChildren().contains(node)) {
                tr.getChildren().remove(node);
            } else {
                removeNode(tr.getChildren(), node);
            }
        }
    }


    public void renameFolder() {
        selectedFolder.setName(newFolderName);
        fileProcess.updateFolder(selectedFolder);
        PrimeFaces.current().executeScript("PF('renameFolderDialog').hide()");
        PrimeFaces.current().ajax().update("form:tree");
        newFolderName = null;

    }

    public void onRowSelect(SelectEvent<File> event) {
        selectedFile = event.getObject();
        FacesMessage msg = new FacesMessage("File Selected", String.valueOf(event.getObject().getName()));
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public void onRowUnselect(UnselectEvent<File> event) {
        selectedFile = null;
        downloadFile = null;
        FacesMessage msg = new FacesMessage("File Unselected", String.valueOf(event.getObject().getName()));
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }


    public void onNodeExpand(NodeExpandEvent event) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Expanded", event.getTreeNode().toString());
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    public void onNodeCollapse(NodeCollapseEvent event) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Collapsed", event.getTreeNode().toString());
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    public void onNodeSelect(NodeSelectEvent event) {
        selectedNode = event.getTreeNode();
        selectedFolder = fileProcess.findFolder((Folder) event.getTreeNode().getData());
        userFiles = selectedFolder.getFiles();
        PrimeFaces.current().ajax().update("files_table:eventsDT");
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Selected", event.getTreeNode().getData().toString());
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    public void onNodeUnselect(NodeUnselectEvent event) {
        userFiles = fileProcess.getFiles();
        selectedFolder = null;
        PrimeFaces.current().ajax().update("files_table:eventsDT");
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Unselected", event.getTreeNode().toString());
        FacesContext.getCurrentInstance().addMessage(null, message);
    }


    public void updateFilesView() {
        if (selectedFolder == null) {
            userFiles = fileProcess.getFiles();
        } else userFiles = fileProcess.getFilesByFolderId(selectedFolder);
    }

    public List<File> getUserFiles() {
        return userFiles;
    }

    public void setUserFiles(List<File> userFiles) {
        this.userFiles = userFiles;
    }

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public UploadedFiles getFiles() {
        return files;
    }

    public void setFiles(UploadedFiles files) {
        this.files = files;
    }

    public StreamedContent getDownloadFile() {
        return downloadFile;
    }

    public void setDownloadFile(StreamedContent downloadFile) {
        this.downloadFile = downloadFile;
    }

    public File getSelectedFile() {
        return selectedFile;
    }

    public void setSelectedFile(File selectedFile) {
        this.selectedFile = selectedFile;
    }

    public TreeNode getRoot() {
        return root;
    }

    public void setRoot(TreeNode root) {
        this.root = root;
    }

    public TreeNode getSelectedNode() {
        return selectedNode;
    }

    public void setSelectedNode(TreeNode selectedNode) {
        this.selectedNode = selectedNode;
    }

    public Folder getSelectedFolder() {
        return selectedFolder;
    }

    public void setSelectedFolder(Folder selectedFolder) {
        this.selectedFolder = selectedFolder;
    }

    public String getNewFolderName() {
        return newFolderName;
    }

    public void setNewFolderName(String newFolderName) {
        this.newFolderName = newFolderName;
    }

    public String getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(String menuItemId) {
        this.menuItemId = menuItemId;
    }

}

