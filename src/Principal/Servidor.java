package Principal;

import java.io.*;
import java.net.*;
import java.util.*;

public final class Servidor {
    public static void main(String[] args) throws Exception {
        // Número da Porta
        int porta = 6500;
        
        // Socket para escuta
        ServerSocket escuta = new ServerSocket(porta);
        
        // Loop infinito para processar requisições
        while(true) {
            // Escuta para requisições
            Socket cliente = escuta.accept();
            
            // Construção de objeto para processar a requisição HTTP
            RequisicaoHttp requisicao = new RequisicaoHttp(cliente);
            
            // Criação de nova tarefa para processar a requisição
            Thread tarefa = new Thread(requisicao);
            
            // Iniciar tarefa
            tarefa.start();
        }
    }
}

final class RequisicaoHttp implements Runnable {
    final static String RCAL = "\r\n";
    Socket cliente;
    
    // Construtor
    public RequisicaoHttp(Socket cliente) throws Exception {
        this.cliente = cliente;
    }
    
    // Implementação do método run()
    public void run() {
        try {
            requisicaoProcesso();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    
    private void requisicaoProcesso() throws Exception {
        // Referência para streams de entrada e saída do socket
        InputStream entrada = cliente.getInputStream();
        DataOutputStream saida = new DataOutputStream(cliente.getOutputStream());
        
        // Filtros de entrada
        InputStreamReader leitor = new InputStreamReader(entrada);
        BufferedReader buffLeitor = new BufferedReader(leitor);
        
        // Linha de requisição da mensagem de requisição HTTP
        String linhaReq = buffLeitor.readLine();
        
        // Exibir a linha de requisição
        System.out.println();
        System.out.println(linhaReq);
        
        // Ler e exibir linhas do cabeçalho
        String linhaCab = null;
        while((linhaCab = buffLeitor.readLine()).length() != 0) {
            System.out.println(linhaCab);
        }
        
        // Extrair o nome do arquivo da linha de requisição
        StringTokenizer tokens = new StringTokenizer(linhaReq);
        tokens.nextToken(); // Pula o método, o qual deve ser "GET"
        String arquivo = tokens.nextToken();
        
        // O arquivo deverá estar dentro do diretório atual
        arquivo = getClass().getResource("").getPath().substring(1).replaceAll("%20", " ") + "Site" + arquivo;
        System.out.println(arquivo);
        
        // Abrir arquivo requisitado
        FileInputStream arqEnt = null;
        boolean existeArq = true;
        try {
            arqEnt = new FileInputStream(arquivo);
        } catch(FileNotFoundException e) {
            existeArq = false;
        }
        
        // Construção da mensagem de resposta
        String linhaStatus = null;
        String linhaTipoConteudo = null;
        String corpo = null;
        
        if(existeArq) {
            linhaStatus = "HTTP/1.1 200 OK" + RCAL;
            linhaTipoConteudo = "Content-type: " + tipoCont(arquivo) + RCAL;
        }
        
        else {
            linhaStatus = "HTTP/1.1 404 Não Encontrado" + RCAL;
            linhaTipoConteudo = "Content-type: Inexistente" + RCAL;
            corpo = "<HTML>" +
                    "<HEAD><TITLE>Não Encontrado</TITLE></HEAD>"+
                    "<BODY>Não Encontrado</BODY></HTML>";
        }
        
        // Enviar linha de status
        saida.writeBytes(linhaStatus);

        // Enviar linha do tipo de conteúdo
        saida.writeBytes(linhaTipoConteudo);

        // Enviar linha em branco para indicar o fim das linhas dos cabeçalho
        saida.writeBytes(RCAL);
        
        // Enviar o corpo da entidade
        if(existeArq) {
            enviaBytes(arqEnt, saida);
            arqEnt.close();
        }
        
        else {
            saida.writeBytes(corpo);
        }
        
        // Fecha streams e sockets
        saida.close();
        buffLeitor.close();
        cliente.close();
    }
    
    private static void enviaBytes(FileInputStream arqEnt, OutputStream saida) throws Exception{
        // Constroi um buffer de 1K para guardar bytes no caminho para o socket
        byte[] buffer = new byte[1024];
        int bytes = 0;
        
        // Copia arquivo requisitado na saída do socket
        while((bytes = arqEnt.read(buffer)) != -1) {
            saida.write(buffer, 0, bytes);
        }
    }
    
    private static String tipoCont(String arquivo) {
        if(arquivo.endsWith(".htm") || arquivo.endsWith(".html")) {
            return "text/html";
        }
        
        if(arquivo.endsWith(".gif")) {
            return "image/gif";
        }
        
        if(arquivo.endsWith(".jpg")) {
            return "image/jpeg";
        }
        
        if(arquivo.endsWith(".swf")) {
            return "other/swf";
        }
        
        return "application/octet-stream";
    }
}