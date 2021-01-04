import java.io.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class Commit extends ObjectStore{
    String msg;
    String timeStamp;
    String lastTreeKey;
    String latestTreeKey;
    String lastCommitKey;
    String latestCommitKey;

    Commit(String message) throws Exception {
        setType("Commit");
        doTimeStamp();
        this.msg = message;
        if(isCommitable())
            createCommit();
    }



    //生成TimeStamp
    private void doTimeStamp(){
        Timestamp time =new Timestamp(System.currentTimeMillis());
        this.timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(time);
    }

    //从HEAD文件中取出现在的branch地址
    private String getCurrBranch() throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(repoPath+File.separator+"jGit"+File.separator+"HEAD"));
        String HEAD=br.readLine();
        String currBranchPath = HEAD.substring(5);
        br.close();
        return currBranchPath;
    }

    //将当前分支的commit key更新
    private void updateCommitKey() throws Exception {
        //相对地址和绝对地址的问题？
        File curr_branch_head = new File(repoPath+ File.separator+"jGit"+File.separator+getCurrBranch());
        //以覆盖的方式向文件中写入最新Commit的key
        FileWriter fw = new FileWriter(curr_branch_head,false);
        fw.write(latestCommitKey);
        fw.flush();
        fw.close();
    }

    protected boolean isCommitable() throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(repoPath+ File.separator+"jGit"+File.separator+getCurrBranch()));
        //读取branch的第一行
        String first_line = br.readLine();
        br.close();
        //生成最新的树,并获取最新的tree key
        latestTreeKey = new Tree("").getKey();
        //如果branch中存有最新一次的commit key，则取出进而取出上一次的tree key
        if(first_line != null){
            lastCommitKey = first_line;
            File file =new File(repoPath+File.separator+objectsSubPath);
            File[] files = file.listFiles();
            for(File f:files){
                if(f.getName() == lastCommitKey){
                    BufferedReader br1 = new BufferedReader(new FileReader(f));
                    String treeKey = br1.readLine();
                    br1.close();
                    lastTreeKey = treeKey.substring(5);
                }
            }
        }
        //branch中没有内容说明之前没有commit过，直接返回true
        else
            return true;

        if(latestTreeKey.equals(lastTreeKey)){
            System.out.println("仓库无变化！");
            return false;
        }
        else
            return true;
    }

    protected void createCommit() throws Exception {
        File commit = new File(repoPath+File.separator+objectsSubPath,"name");
        StringBuilder sb = new StringBuilder();
        sb.append("tree "+latestTreeKey+"\n");
        //如果有parent commit 则写入，没有则跳过这行
        if(lastCommitKey !=null)
            sb.append("parent "+lastCommitKey+"\n");
        sb.append("Time: "+timeStamp+"\n");
        sb.append(msg);
        FileWriter fr = new FileWriter(commit);
        fr.write(sb.toString());
        fr.flush();
        fr.close();

        //计算新commit的key,并命其为文件名
        latestCommitKey = new Hash(commit).getHashcode();
        commit.renameTo(new File(repoPath+File.separator+objectsSubPath,latestCommitKey));
        //更新Head
        updateCommitKey();
    }
}
