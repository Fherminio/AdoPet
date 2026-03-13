package bd;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import javax.imageio.ImageIO;
import backend.Usuario;
import backend.SecurityUtils;
import frontend.Gui;
import javafx.embed.swing.SwingFXUtils;
import backend.Pair;
import backend.Pet;
/**
 * Essa classe representa todas as funcoes que so recorrentes e que gerenciam o Banco de Dados
 *
 * @authors  Mateus Prado, Mateus Tomieiro, Victor Reis, Matheus Rigato
 *
 */

public class BDConexaoClass{

    DriverManager driver;

    /*
     * BDConexao - Faz conexao com o Banco de Dados
     * @return - Objeto Connection para executar as Queries
     */

    public static Connection BDConexao(){
        Properties props = new Properties();
        try (InputStream input = BDConexaoClass.class.getResourceAsStream("/db.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find db.properties");
                return null;
            }
            props.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }

       String con = props.getProperty("db.url");
       String server_user = props.getProperty("db.user");
       String server_pass = props.getProperty("db.password");

       Connection connect = null;
       try {
	   connect = DriverManager.getConnection(con,server_user,server_pass);
       } catch (SQLException e) {
	   System.out.println("Erro ao conectar com o BD");
           e.printStackTrace();
       }
       return connect;

    }

    /*
     * getIdAnun - funcao que retorna o id do usuario que tem determinado nome de usuario
     * @param username - Recebe o nome de usuario
     * @return id do usuario
     *
     */
    public static int getIdAnun(String username) {
	String select = "SELECT cliente_id FROM clientes WHERE username=?";
	try (Connection con = BDConexao();
             PreparedStatement ps = con.prepareStatement(select)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.last()) {
                    return rs.getInt(1);
                }
            }
		} catch (SQLException e) {
			System.out.println("Erro getIdAnun");
            e.printStackTrace();
		}
		return 0;
    }
    /*
     * cadastroPet - funcao que adiciona pet no Banco de Dados
     * @param p - recebe um Objeto Pet
     */
    public static void cadastroPet(Pet p){
        String insert = "INSERT INTO pets(species,nome,sexo,detalhes,id_doador,imagem) values(?,?,?,?,?,?)";
        try (Connection con = BDConexao();
             PreparedStatement ps = con.prepareStatement(insert)) {
			ps.setString(1, p.getEspecie());
			ps.setString(2, p.getNome());
	        ps.setString(3, p.getSexo());
	        ps.setString(4, p.getDetalhes());
	        int idAnun = getIdAnun(Gui.User.getUserName());
	        ps.setInt(5,idAnun);
	        BufferedImage image = SwingFXUtils.fromFXImage(p.getIcone(), null);
	        ByteArrayOutputStream A = new ByteArrayOutputStream();
	        try {
				ImageIO.write(image, "jpg", A);
			} catch (IOException e) {
				System.out.println("Erro ao escrever a imagem!");
                e.printStackTrace();
			}
	        InputStream is = new ByteArrayInputStream(A.toByteArray());
	        ps.setBlob(6, is);
			ps.execute();
		} catch (SQLException e) {
			System.out.println("Erro cadastroPet");
            e.printStackTrace();
		}
    }

    /*
     * cadastroUser - Adiciona um usuario no Banco de Dados
     * @param user - Recebe um Objeto Usuario
     */
    public static void cadastroUser(Usuario user){
        String insert = "INSERT INTO clientes(username,senha,nome,cpf,cidade,endereco,cep) values(?,?,?,?,?,?,?)";
        try (Connection con = BDConexao();
             PreparedStatement ps = con.prepareStatement(insert)) {
			ps.setString(1, user.getUserName());
            String hashedPass = SecurityUtils.hashPassword(user.getSenha());
	        ps.setString(2, hashedPass);
	        ps.setString(3, user.getNome());
	        ps.setString(4, user.getCpf());
	        ps.setString(5, user.getCidade());
	        ps.setString(6, user.getEndereco());
	        ps.setString(7, user.getCep());
			ps.execute();
		} catch (SQLException e) {
			System.out.println("Erro cadastroUser");
            e.printStackTrace();
		}
    }
    /**
     * loginUser - Faz a funcao para verificar os dados inseridos em uma row especifica
     * @param user - Recebe objeto Usuario
     * @return retorna true se a verificacao deu certo
     */
    public static boolean loginUser(Usuario user){
        String select = "SELECT * FROM clientes where username=?";
        try (Connection con = BDConexao();
             PreparedStatement ps = con.prepareStatement(select)) {
            ps.setString(1, user.getUserName());
            try (ResultSet rs = ps.executeQuery()) {
                while(rs.next()){
                    String  getpass = rs.getString(3);
                    if(SecurityUtils.checkPassword(user.getSenha(), getpass)){
                        //Autoriza login, pois o usuario esta no BD;
                        return true;
                    }
                }
            }
		} catch (SQLException e) {
			System.out.println("Erro loginUser");
            e.printStackTrace();
		}
        //Se nao encontrar nenhuma senha igual, nega login!
        return false;
    }
    /*
     * getPetsFromChat - pega todos os chats iniciados com um determinado usuario
     * @param user1 - Recebe o primeiro usuario
     * @param user2 - Recebe o segundo usuario
     * @return - retorna um Vetor de Pets
     */
    public static Vector<Pet> getPetsFromChat(Usuario User1, Usuario User2){
	Vector<Pet> pets = new Vector<Pet>();
        String select = "SELECT pet_id FROM chat WHERE (user1_id = ? AND user2_id = ?)";
        try (Connection con = BDConexao();
             PreparedStatement ps = con.prepareStatement(select)) {
            ps.setInt(1, BDConexaoClass.getIdAnun(User1.getUserName()));
			ps.setInt(2, BDConexaoClass.getIdAnun(User2.getUserName()));
            try (ResultSet rs = ps.executeQuery()) {
                while(rs.next()){
                    pets.add(BDConexaoClass.getPet(rs.getInt(1)));
                }
            }
		} catch (SQLException e) {
			System.out.println("Erro getPetsFromChat");
            e.printStackTrace();
		}
	return pets;
    }
    /*
     * getPet - Retorna um objeto Pet
     * @param id - Recebe um id de um pet especifico
     * @return - Retorna um  objeto Pet
     */
    public static Pet getPet(int id){
		Pet p = new Pet();
		String select = "SELECT * FROM pets WHERE pet_id=?";
        try (Connection con = BDConexao();
             PreparedStatement ps = con.prepareStatement(select)) {
			ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while(rs.next()){
                    p.setPetID(rs.getInt(1));
                    p.setEspecie(rs.getString(2));
                    p.setNome(rs.getString(3));
                    p.setSexo(rs.getString(4));
                    p.setDetalhes(rs.getString(5));
                    p.setAnuncianteID(rs.getInt(6));
                    Usuario anunciante = new Usuario();
                    anunciante = retornaUsuario((int)p.getAnuncianteID());
                    p.setAnunciante(anunciante);
                }
                if(rs.last()){
                    InputStream in = rs.getBlob(7).getBinaryStream();
                    BufferedImage image = ImageIO.read(in);
                    p.setIcone(SwingFXUtils.toFXImage(image, null));
                }
            }
		} catch (SQLException | IOException e) {
			System.out.println("Erro getPet");
            e.printStackTrace();
			return null;
		}
        return p;
	}


    /*
     * usuarioAceitou - Verifica se o usuario clicou no botao Finalizar
     * @param user1 - Recebe o Objeto Usuario
     * @param p - Recebe o Objeto Pet
     * @return - Retorna se a verificacao deu certo ou errado
     */
    public static boolean UsuarioAceitou(Usuario user1, Pet p) {
	String select = "SELECT confirma_user1 FROM chat WHERE pet_id=? AND user1_id=?";
	try (Connection con = BDConexao();
             PreparedStatement pss = con.prepareStatement(select)) {
		pss.setInt(1,(int)p.getPetID());
			pss.setInt(2,BDConexaoClass.getIdAnun(user1.getUserName()));
            try (ResultSet rss = pss.executeQuery()) {
                if(rss.first()) {
                    if(rss.getBoolean(1)) {
                        return true;
                    }
                }
            }
	} catch(SQLException e) {
		System.out.println("Erro UsuarioAceitou");
            e.printStackTrace();
	}
	return false;
    }

   /**
    * setaFinalizado - Seta no Banco de Dados que o Usuario clicou no botao Finalizar
    * @param user1 - Recebe o primeiro Usuario
    * @param user2 - Recebe o segundo Usuario
    * @param p - Recebe o Objeto Pet
    */
    public static void setaFinalizado(Usuario user1, Usuario user2, Pet p) {
	String up1= "UPDATE chat SET confirma_user1=true where (user1_id=? AND user2_id=? AND pet_id=?)";
	try (Connection con = BDConexao();
             PreparedStatement psu1 = con.prepareStatement(up1)) {
		psu1.setInt(1,BDConexaoClass.getIdAnun(user1.getUserName()));
		psu1.setInt(2,BDConexaoClass.getIdAnun(user2.getUserName()));
		psu1.setInt(3, (int)p.getPetID());
		psu1.execute();
	}catch(SQLException e) {
		System.out.println("Erro setaFinalizado");
            e.printStackTrace();
        }
    }

    /**
     * excluirChat - Exclui as mensagens de um determinado chat
     * @param user1 - Recebe o primeiro Usuario
     * @param user2 - Recebe o segundo Usuario
     */

    public static void excluirChat(Usuario user1, Usuario user2, Pet p){
		String excluirMensagens = "DELETE FROM mensagens WHERE id_chat=?";
		String selectChat1 = "SELECT chat_id FROM chat WHERE (user1_id=? AND user2_id=? AND pet_id=?)";
		String deletarChat1 = "DELETE FROM chat WHERE chat_id=?";

        try (Connection con = BDConexao()) {
            int ChatId = 0;
            try (PreparedStatement ps = con.prepareStatement(selectChat1)) {
                ps.setInt(1, BDConexaoClass.getIdAnun(user1.getUserName()));
                ps.setInt(2, BDConexaoClass.getIdAnun(user2.getUserName()));
                ps.setInt(3, (int)p.getPetID());
                try (ResultSet chat_id = ps.executeQuery()) {
                    if(chat_id.first()) {
                        ChatId = chat_id.getInt(1);
                    }
                }
            }
            try (PreparedStatement psd = con.prepareStatement(excluirMensagens)) {
                psd.setInt(1, ChatId);
                psd.execute();
            }
            try (PreparedStatement ps = con.prepareStatement(deletarChat1)) {
                ps.setInt(1, ChatId);
                ps.execute();
            }
            try (PreparedStatement ps = con.prepareStatement(deletarChat1)) {
                ps.setInt(1, (ChatId+1));
                ps.execute();
            }
        } catch (SQLException e) {
            System.out.println("Erro excluirChat");
            e.printStackTrace();
        }
	}

    /*
     * excluirTodosChats - Exclui todos os chat que tem um determinado petId
     * @param id - Recebe o id do Pet
     *
     */
    public static void excluirTodosChats(int id){
		String select = "SELECT chat_id FROM chat WHERE pet_id=?";
		String delMensagens = "DELETE FROM mensagens WHERE id_chat=?";
		String delChats = "DELETE FROM chat WHERE pet_id=?";

        try (Connection con = BDConexao()) {
            Vector<Integer> chatIds = new Vector<>();
            try (PreparedStatement ps = con.prepareStatement(select)) {
                ps.setInt(1,id);
                try (ResultSet rs = ps.executeQuery()) {
                    while(rs.next()){
                        chatIds.add(rs.getInt(1));
                    }
                }
            }
            for (int chatId : chatIds) {
                try (PreparedStatement psdm = con.prepareStatement(delMensagens)) {
                    psdm.setInt(1, chatId);
                    psdm.execute();
                }
            }
            try (PreparedStatement psdc = con.prepareStatement(delChats)) {
                psdc.setInt(1,id);
                psdc.execute();
            }
        } catch (SQLException e) {
            System.out.println("Erro excluirTodosChats");
            e.printStackTrace();
        }
	}
    /**
     * adotarPet - Deleta o pet do Banco de Dados
     * @param user1 - Recebe o primeiro Usuario
     * @param user2 - Recebe o segundo Usuario
     * @param petId - Recebe o id do Pet
     */
    public static void adotarPet(Usuario user1, Usuario user2, int petId){
	excluirTodosChats(petId);
        String del = "Delete FROM pets WHERE pet_id = ?";
		String up = "ALTER TABLE pets AUTO_INCREMENT=?";
		String pegarId = "Select pet_id FROM pets";

        try (Connection con = BDConexao()) {
            try (PreparedStatement ps = con.prepareStatement(del)) {
                ps.setInt(1,petId);
                ps.execute();
            }
            int id = 0;
            try (PreparedStatement pss = con.prepareStatement(pegarId);
                 ResultSet rss = pss.executeQuery()) {
                if(rss.last()) {
                    id = rss.getInt(1);
                } else {
                    return;
                }
            }
            try (PreparedStatement psu = con.prepareStatement(up)) {
                psu.setInt(1,id-1);
                psu.execute();
            }
        } catch (SQLException e) {
            System.out.println("Erro adotarPet");
            e.printStackTrace();
        }
    }
    /*
     * Verifica se existe algum chat criado entre dois usuarios
     * @param user1 - Recebe o primeiro usuario
     * @param user2 - Recebe o segundo usuario
     * @return Retorna um boolean que demonstra a existncia do chat(ou no) no Banco de Dados
     *
     */

    public static boolean existeChat(Usuario user1,Usuario user2){
        String select = "SELECT * FROM chat WHERE ((user1_id=? AND user2_id=?) OR (user1_id=? AND user2_id=?)) AND pet_id=?";
        try (Connection con = BDConexao();
             PreparedStatement ps = con.prepareStatement(select)) {
			ps.setInt(1, BDConexaoClass.getIdAnun(user1.getUserName()));
			ps.setInt(2, BDConexaoClass.getIdAnun(user2.getUserName()));
			ps.setInt(3, BDConexaoClass.getIdAnun(user2.getUserName()));
			ps.setInt(4, BDConexaoClass.getIdAnun(user1.getUserName()));
			ps.setInt(5, (int)Gui.petCorrente.getPetID());
            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()){
                    return true;
                }
            }
		} catch (SQLException e) {
			System.out.println("Erro existeChat");
            e.printStackTrace();
		}
        return false;
    }

    /*
     * cria uma query que adiciona um novo chat no Banco de Dados
     * @param user1 - Primeiro usuario participante do chat
     * @param user2 - Segundo usuario participante do chat
     *
     */

    public static void comecarChat(Usuario user1,Usuario user2){
        String insert = "INSERT INTO chat(user1_id,user2_id,confirma_user1,confirma_user2,pet_id) Values(?,?,?,?,?)";
        String insert2 = "INSERT INTO chat(user1_id,user2_id,confirma_user1,confirma_user2,pet_id) Values(?,?,?,?,?)";
        try (Connection con = BDConexao();
             PreparedStatement psi = con.prepareStatement(insert);
             PreparedStatement psi2 = con.prepareStatement(insert2)) {
			psi.setInt(1, BDConexaoClass.getIdAnun((user1.getUserName())));
			psi.setInt(2, BDConexaoClass.getIdAnun((user2.getUserName())));
			psi.setBoolean(3, false);
			psi.setBoolean(4, false);
			psi.setInt(5, (int)Gui.petCorrente.getPetID());
			psi2.setInt(1, BDConexaoClass.getIdAnun((user2.getUserName())));
			psi2.setInt(2, BDConexaoClass.getIdAnun((user1.getUserName())));
			psi2.setBoolean(3, false);
			psi2.setBoolean(4, false);
			psi2.setInt(5, (int)Gui.petCorrente.getPetID());
			psi.execute();
			psi2.execute();
		} catch (SQLException e) {
			System.out.println("Erro comecarChat");
            e.printStackTrace();
		}
    }

    /*
     * Retorna todas as mensagens que dois usuarios j tiveram entre si
     * @param user1 - Primeiro usuario participante do chat
     * @param user2 - Segundo usuario participante do chat
     * @return Um vector que contem os ids dos usuarios e as suas respectivas mensagens
     *
     */
    public static Vector<Pair<Integer,String>> getMensagensAntigas(Usuario user1, Usuario user2){
        String select = "SELECT chat_id FROM chat WHERE ((user1_id=? AND user2_id=?) OR (user2_id=? AND user1_id=?)) AND pet_id=?";
        String puxarMensagens = "Select mensagem,id_remetente FROM mensagens INNER JOIN chat ON chat.chat_id = mensagens.id_chat WHERE chat.chat_id = ?";
        Vector<Pair<Integer,String>> pares = new Vector<Pair<Integer, String>>();
        try (Connection con = BDConexao();
             PreparedStatement ps = con.prepareStatement(select)) {
			ps.setInt(1, BDConexaoClass.getIdAnun(user1.getUserName()));
			ps.setInt(2, BDConexaoClass.getIdAnun(user2.getUserName()));
			ps.setInt(3, BDConexaoClass.getIdAnun(user1.getUserName()));
			ps.setInt(4, BDConexaoClass.getIdAnun(user2.getUserName()));
			ps.setInt(5, (int)Gui.petCorrente.getPetID());
            int id = 0;
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.first()) {
                    id = rs.getInt(1);
                }
            }
            if (id != 0) {
                try (PreparedStatement psp = con.prepareStatement(puxarMensagens)) {
                    psp.setInt(1,id);
                    try (ResultSet rsp = psp.executeQuery()) {
                        while(rsp.next()){
                            String msg = rsp.getString(1);
                            int sent = rsp.getInt(2);
                            pares.add(new Pair<Integer,String>(sent,msg));
                        }
                    }
                }
            }
		} catch (SQLException e) {
			System.out.println("Erro getMensagensAntigas");
            e.printStackTrace();
		}
        return pares;
    }

    /*
     * Cria uma query que coloca uma mensagem de acordo com o chat de dois usuarios
     * @param user1 - Primeiro participante do chat
     * @param user2 - Segundo participante do chat
     * @param mensagem - Mensagem que foi enviada
     *
     */

    public static void criarMensagem(Usuario user1, Usuario user2,String mensagem){
        String pegarID = "Select chat_id FROM chat WHERE ((user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?)) AND pet_id=?";
        String inserirMensagem = "Insert INTO mensagens(id_chat,id_remetente,mensagem) VALUES(?,?,?)";
        try (Connection con = BDConexao();
             PreparedStatement psc = con.prepareStatement(pegarID)) {
			psc.setInt(1,BDConexaoClass.getIdAnun(user1.getUserName()));
			psc.setInt(2,BDConexaoClass.getIdAnun(user2.getUserName()));
			psc.setInt(3,BDConexaoClass.getIdAnun(user2.getUserName()));
			psc.setInt(4,BDConexaoClass.getIdAnun(user1.getUserName()));
			psc.setInt(5,(int)Gui.petCorrente.getPetID());
            int chat = 0;
            try (ResultSet rsc = psc.executeQuery()) {
                if(!rsc.next())
                        return;
                rsc.first();
                chat = rsc.getInt(1);
            }
            try (PreparedStatement psi = con.prepareStatement(inserirMensagem)) {
                psi.setInt(1,chat);
                psi.setInt(2,BDConexaoClass.getIdAnun(user2.getUserName()));
                psi.setString(3,mensagem);
                psi.execute();
            }
		} catch (SQLException e) {
			System.out.println("Erro criarMensagem");
            e.printStackTrace();
		}
    }

    /*
     * getUserFromId - retorna o objeto User
     * @param id - recebe o id ddo usuario
     * @return - retorna o Objeto Usuario
     *
     */

    private static Usuario getUserFromId(int id) {
	Usuario aux = new Usuario();
	String cont = "SELECT * FROM clientes WHERE cliente_id = ?";
	try (Connection con = BDConexao();
             PreparedStatement ps = con.prepareStatement(cont)) {
			ps.setInt(1,id);
            try (ResultSet rs = ps.executeQuery()) {
                while(rs.next()){
                    aux.setId(id);
                    aux.setUserName(rs.getString(2));
                    aux.setSenha(rs.getString(3));
                    aux.setNome(rs.getString(4));
                    aux.setCpf(rs.getString(5));
                    aux.setCidade(rs.getString(6));
                    aux.setEndereco(rs.getString(7));
                    aux.setCep(rs.getString(8));
                }
            }
		} catch (SQLException e) {
			System.out.println("Erro getUserFromId");
            e.printStackTrace();
		}
		return aux;
	}
    /**
     * getSizeUser - Retorna o total de todos os contatos que determinado usuario conversou
     * @param user1 - Recebe o usuario
     * @return - retorna o total de contatos
     */

    public static int getSizeUser(Usuario user1){
	String cont = "SELECT DISTINCTROW(user2_id) FROM chat WHERE user1_id = ?";
	try (Connection con = BDConexao();
             PreparedStatement ps = con.prepareStatement(cont)) {
			ps.setInt(1,BDConexaoClass.getIdAnun(user1.getUserName()));
            try (ResultSet rs = ps.executeQuery()) {
                int aux = 0;
                while(rs.next()) {
                    aux++;
                }
                return aux;
            }
		} catch (SQLException e) {
			System.out.println("Erro getSizeUser");
            e.printStackTrace();
		}
	return 0;
    }

    /**
     * getSizePets - Retorna o total de pets no Banco de Dados
     * @return - retorna o total de pets no Banco de Dados
     */

    public static int getSizePets(){
	String cont = "SELECT COUNT(*) FROM pets";
	try (Connection con = BDConexao();
             PreparedStatement ps = con.prepareStatement(cont);
             ResultSet rs = ps.executeQuery()) {
		if (rs.last()) {
                return rs.getInt(1);
            }
		} catch (SQLException e) {
			System.out.println("Erro getSizePets");
            e.printStackTrace();
		}
        return 0;
    }


    /**
     * listaContatos - Lista os contatos de um determinado usuario
     * @param user1 - Recebe o Objeto Usuario
     * @return - Retorna um vetor de Usuario
     */
    public static Usuario[] listaContatos(Usuario user1){
	String select = "SELECT DISTINCTROW(user2_id) FROM chat WHERE user1_id=?";
        int len = getSizeUser(user1);
        Usuario[] pessoa = new Usuario[len];
	try (Connection con = BDConexao();
             PreparedStatement ps = con.prepareStatement(select)) {
			ps.setInt(1,BDConexaoClass.getIdAnun(user1.getUserName()));
            try (ResultSet rs = ps.executeQuery()) {
                int i = 0;
                while(rs.next()){
                    pessoa[i] = getUserFromId(rs.getInt(1));
                    i++;
                }
            }
		} catch (SQLException e) {
			System.out.println("Erro listaContatos");
            e.printStackTrace();
		}
	return pessoa;
    }


	public static Usuario retornaUsuario(int id) throws SQLException{
        Usuario user = new Usuario();
        String select = "SELECT * FROM clientes WHERE cliente_id = ?";
        try (Connection con = BDConexao();
             PreparedStatement ps = con.prepareStatement(select)) {
            ps.setInt(1,id);
            try (ResultSet rs = ps.executeQuery()) {
                while(rs.next()){
                    user.setId(rs.getInt(1));
                    user.setUserName(rs.getString(2));
                    user.setSenha(rs.getString(3));
                    user.setNome(rs.getString(4));
                    user.setCpf(rs.getString(5));
                    user.setCidade(rs.getString(6));
                    user.setEndereco(rs.getString(7));
                    user.setCep(rs.getString(8));
                }
            }
        }
        return user;
    }
	/**
	 * retornaPet - Retorna o  Objeto pet
	 * @param index - Recebe o index do Pet no Banco de Dados
	 * @return - Retorna o Objeto Pet
	 */
    public static Pet retornaPet(int index){
        String select = "SELECT * FROM pets LIMIT ?,1";
        Pet p = new Pet();
        try (Connection con = BDConexao();
             PreparedStatement ps = con.prepareStatement(select)) {
			ps.setInt(1, index);
            try (ResultSet rs = ps.executeQuery()) {
                while(rs.next()){
                    p.setPetID(rs.getInt(1));
                    p.setEspecie(rs.getString(2));
                    p.setNome(rs.getString(3));
                    p.setSexo(rs.getString(4));
                    p.setDetalhes(rs.getString(5));
                    p.setAnuncianteID(rs.getInt(6));
                    Usuario anunciante = new Usuario();
                    anunciante = retornaUsuario((int)p.getAnuncianteID());
                    p.setAnunciante(anunciante);
                }
                if(rs.last()){
                    InputStream in = rs.getBlob(7).getBinaryStream();
                    BufferedImage image = ImageIO.read(in);
                    p.setIcone(SwingFXUtils.toFXImage(image, null));
                }
            }
		} catch (SQLException | IOException e) {
			System.out.println("Erro retornaPet");
            e.printStackTrace();
			return null;
		}
        return p;
    }

    /**
     * retornaPetsDisponiveis - Retorna uma lista de Pets
     * @param offset - O deslocamento no Banco de Dados
     * @param limit - O numero maximo de Pets a retornar
     * @return - Um array de Pets (pode conter elementos nulos)
     */
    public static Pet[] retornaPetsDisponiveis(int offset, int limit) {
        String select = "SELECT * FROM pets LIMIT ?,?";
        Pet[] pets = new Pet[limit];
        try (Connection con = BDConexao();
             PreparedStatement ps = con.prepareStatement(select)) {
            ps.setInt(1, offset);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                int i = 0;
                while (rs.next() && i < limit) {
                    Pet p = new Pet();
                    p.setPetID(rs.getInt(1));
                    p.setEspecie(rs.getString(2));
                    p.setNome(rs.getString(3));
                    p.setSexo(rs.getString(4));
                    p.setDetalhes(rs.getString(5));
                    p.setAnuncianteID(rs.getInt(6));
                    p.setAnunciante(retornaUsuario((int) p.getAnuncianteID()));

                    InputStream in = rs.getBlob(7).getBinaryStream();
                    BufferedImage image = ImageIO.read(in);
                    p.setIcone(SwingFXUtils.toFXImage(image, null));

                    pets[i] = p;
                    i++;
                }
            }
        } catch (SQLException | IOException e) {
            System.out.println("Erro retornaPetsDisponiveis");
            e.printStackTrace();
        }
        return pets;
    }

}
