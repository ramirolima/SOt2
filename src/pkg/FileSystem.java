package pkg;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Scanner;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.FilterInputStream;

public class FileSystem {
	static int block_size = 1024;
	static int blocks = 2048;
	static int fat_size = blocks * 2;
	static int fat_blocks = fat_size / block_size;
	static int root_block = fat_blocks;
	static int dir_entry_size = 32;
	static int dir_entries = block_size / dir_entry_size;

	/* FAT data structure */
	final static short[] fat = new short[blocks];
	/* data block */
	final static byte[] data_block = new byte[block_size];

	/* reads a data block from disk */
	public static byte[] readBlock(String file, int block) {
		byte[] record = new byte[block_size];
		try {
			RandomAccessFile fileStore = new RandomAccessFile(file, "rw");
			fileStore.seek(block * block_size);
			fileStore.read(record, 0, block_size);
			fileStore.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return record;
	}

	/* writes a data block to disk */
	public static void writeBlock(String file, int block, byte[] record) {
		try {
			RandomAccessFile fileStore = new RandomAccessFile(file, "rw");
			fileStore.seek(block * block_size);
			fileStore.write(record, 0, block_size);
			fileStore.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* reads the FAT from disk */
	public static short[] readFat(String file) {
		short[] record = new short[blocks];
		try {
			RandomAccessFile fileStore = new RandomAccessFile(file, "rw");
			fileStore.seek(0);
			for (int i = 0; i < blocks; i++)
				record[i] = fileStore.readShort();
			fileStore.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return record;
	}

	/* writes the FAT to disk */
	public static void writeFat(String file, short[] fat) {
		try {
			RandomAccessFile fileStore = new RandomAccessFile(file, "rw");
			fileStore.seek(0);
			for (int i = 0; i < blocks; i++)
				fileStore.writeShort(fat[i]);
			fileStore.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* reads a directory entry from a directory */
	public static DirEntry readDirEntry(int block, int entry) {
		byte[] bytes = readBlock("filesystem.dat", block);
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		DataInputStream in = new DataInputStream(bis);
		DirEntry dir_entry = new DirEntry();

		try {
			in.skipBytes(entry * dir_entry_size);

			for (int i = 0; i < 25; i++)
				dir_entry.filename[i] = in.readByte();
			dir_entry.attributes = in.readByte();
			dir_entry.first_block = in.readShort();
			dir_entry.size = in.readInt();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return dir_entry;
	}

	/* writes a directory entry in a directory */
	public static void writeDirEntry(int block, int entry, DirEntry dir_entry) {
		byte[] bytes = readBlock("filesystem.dat", block);
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		DataInputStream in = new DataInputStream(bis);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bos);

		try {
			for (int i = 0; i < entry * dir_entry_size; i++)
				out.writeByte(in.readByte());

			for (int i = 0; i < dir_entry_size; i++)
				in.readByte();

			for (int i = 0; i < 25; i++)
				out.writeByte(dir_entry.filename[i]);
			out.writeByte(dir_entry.attributes);
			out.writeShort(dir_entry.first_block);
			out.writeInt(dir_entry.size);

			for (int i = entry + 1; i < entry * dir_entry_size; i++)
				out.writeByte(in.readByte());
		} catch (IOException e) {
			e.printStackTrace();
		}

		byte[] bytes2 = bos.toByteArray();
		for (int i = 0; i < bytes2.length; i++)
			data_block[i] = bytes2[i];
		writeBlock("filesystem.dat", block, data_block);
	}
	
	public static void init() {
		/* initialize the FAT */
		for (int i = 0; i < fat_blocks; i++)
			fat[i] = 0x7ffe;
		fat[root_block] = 0x7fff;
		for (int i = root_block + 1; i < blocks; i++)
			fat[i] = 0;
		/* write it to disk */
		writeFat("filesystem.dat", fat);

		/* initialize an empty data block */
		for (int i = 0; i < block_size; i++)
			data_block[i] = 0;

		/* write an empty ROOT directory block */
		writeBlock("filesystem.dat", root_block, data_block);

		/* write the remaining data blocks to disk */
		for (int i = root_block + 1; i < blocks; i++)
			writeBlock("filesystem.dat", i, data_block);
	}
	
	public static void load() {
		
		/* fill three root directory entries and list them */
		DirEntry dir_entry = new DirEntry();
		
		
		String name = "file1";
		byte[] namebytes = name.getBytes();
		for (int i = 0; i < namebytes.length; i++)
			dir_entry.filename[i] = namebytes[i];
		dir_entry.attributes = 0x01;
		dir_entry.first_block = 5;
		dir_entry.size = 222;
		writeDirEntry(root_block, 0, dir_entry);
		 
		name = "";
		namebytes = new byte[1];
		dir_entry = new DirEntry();
		name = "diretorio1";
		namebytes = name.getBytes();
		for (int i = 0; i < namebytes.length; i++)
			dir_entry.filename[i] = namebytes[i];
		dir_entry.attributes = 0x02;
		dir_entry.first_block = 6;
		dir_entry.size = 555;
		writeDirEntry(root_block, 1, dir_entry);
		
	name = "";
		namebytes = new byte[1];
		dir_entry = new DirEntry();
		name = "diretorio1.1";
		namebytes = name.getBytes();
		for (int i = 0; i < namebytes.length; i++)
			dir_entry.filename[i] = namebytes[i];
		dir_entry.attributes = 0x02;
		dir_entry.first_block = 7;
		dir_entry.size = 555;
		writeDirEntry(6, 1, dir_entry);
		
		name = "";
		namebytes = new byte[1];
		dir_entry = new DirEntry();
		name = "file2";
		namebytes = name.getBytes();
		for (int i = 0; i < namebytes.length; i++)
			dir_entry.filename[i] = namebytes[i];
		dir_entry.attributes = 0x01;
		dir_entry.first_block = 8;
		dir_entry.size = 222;
		writeDirEntry(6, 3, dir_entry);
		
		name = "";
		namebytes = new byte[1];
		dir_entry = new DirEntry();
		name = "file3";
		namebytes = name.getBytes();
		for (int i = 0; i < namebytes.length; i++)
			dir_entry.filename[i] = namebytes[i];
		dir_entry.attributes = 0x01;
		dir_entry.first_block = 9;
		dir_entry.size = 222;
		writeDirEntry(7, 3, dir_entry);
	}
	
	public static byte[] equality(byte[] in) {		
		byte[] out = new byte[25];		
		for(int i = 0;i<in.length;i++)
			out[i]=in[i];
		return out;		
	}
	
	public static int discoveryFreeEntry(int block) {		
		DirEntry dir_entry = new DirEntry();
		
		for (int i = 0; i < dir_entries; ++i) {				
			dir_entry = readDirEntry(block, i);			
			if(dir_entry.attributes == 0x00)
				return i;			
		}
		
	return -1;
	}
	
	public static int discoveryFreeFat() {

        for(int i=root_block+1; i<fat.length; i++)
        	if(fat[i] == 0) return (short) i;        
        
        return -1;
	}
	
	public static int discoveryBlock(String[] path) {
		DirEntry dir_entry = new DirEntry();
		int currBlock = root_block;
		Boolean find = true;
		
		if(!(path.length == 1 && path[0] == ""))
		for(int i=0;i<path.length; i++) {
			find = false;
			byte[] pathByte = equality(path[i].getBytes());
			
			for (int x = 0; x < dir_entries; ++x) {				
				dir_entry = readDirEntry(currBlock, x);
				
				if(dir_entry.attributes == 0x02)				
					if(Arrays.equals(dir_entry.filename, pathByte)) {
						find = true;
						currBlock = dir_entry.first_block;
						break;
					}
			}
		}
		
		return find ? currBlock : -1;		
	}
	
	public static void ls(String inPath) {			
		String[] path = inPath.split("/");
		DirEntry dir_entry = new DirEntry();
		int block = discoveryBlock(path);
		
		if(block != -1) {
			for (int i = 0; i < dir_entries; i++) {
				dir_entry = readDirEntry(block, i);
				if(dir_entry.attributes != 0x00)
					if(dir_entry.attributes != 0x01)				
						System.out.println("Dir: " + new String(dir_entry.filename));
					else if(dir_entry.attributes != 0x02)
						System.out.println("File: " +new String(dir_entry.filename));				
			}
		}
		else
			System.out.println("Caminho inexistente!");
	}
	
	public static void mkdir(String inPath, String name) {			
		String[] path = inPath.split("/");
		DirEntry dir_entry = new DirEntry();
		int block = discoveryBlock(path);
		int freeEntry;
		int freeFat = discoveryFreeFat();
		fat[freeFat] = 0x7fff;
		writeFat("filesystem.dat",fat);
		
		
		if(freeFat != -1) {
			if(block != -1) { 	
				freeEntry = discoveryFreeEntry(block);	
				if(freeEntry != -1) {
					dir_entry = new DirEntry();				
					byte[] namebytes = name.getBytes();
					for (int i = 0; i < namebytes.length; i++)
						dir_entry.filename[i] = namebytes[i];
					dir_entry.attributes = 0x02;
					dir_entry.first_block = (short)freeFat;
					dir_entry.size = 0;
					writeDirEntry(block, freeEntry, dir_entry);
				}
				else
					System.out.println("Espaço insuficiente no bloco!");
			}
			else
				System.out.println("Caminho inexistente!");
		}
		else
			System.out.println("Espaço insuficiente no disco!");
	}
	
	public static void create(String inPath, String name, int size, String content) {
		String[] path = inPath.split("/");
		DirEntry dir_entry = new DirEntry();
		int block = discoveryBlock(path);
		int freeEntry;
		
		if(block != -1) { 	
			freeEntry = discoveryFreeEntry(block);	
			if(freeEntry != -1) {					
				if(size <= 1024)
				{
					int freeFat = discoveryFreeFat();						
					fat[freeFat] = 0x7fff;
					writeFat("filesystem.dat",fat);
					
					dir_entry = new DirEntry();				
					byte[] namebytes = name.getBytes();
					for (int i = 0; i < namebytes.length; i++)
						dir_entry.filename[i] = namebytes[i];
					dir_entry.attributes = 0x01;
					dir_entry.first_block = (short)freeFat;
					dir_entry.size = size;
					writeDirEntry(block, freeEntry, dir_entry);
											
                    byte[] arrContent = content.getBytes();                        
                    System.arraycopy(arrContent, 0, data_block, 0, arrContent.length);
                    writeBlock("filesystem.dat",freeFat, data_block);						
				}
				else {
					int nBlocks = (int)Math.ceil(size/1024.0);
					int[] fatBlocks = new int[nBlocks];
					
					for(int i = 0; i < nBlocks; i++) {											
						fatBlocks[i] = discoveryFreeFat();
						fat[fatBlocks[i]] = 0x7fff;
					}
						
					for(int i = 0; i < fatBlocks.length -1; i++)						
						fat[fatBlocks[i]] = (short)fatBlocks[i+1];	
					
					dir_entry = new DirEntry();				
					byte[] namebytes = name.getBytes();
					for (int i = 0; i < namebytes.length; i++)
						dir_entry.filename[i] = namebytes[i];
					dir_entry.attributes = 0x01;
					dir_entry.first_block = (short)fatBlocks[0];
					dir_entry.size = size;
					writeDirEntry(block, freeEntry, dir_entry);
					
					writeFat("filesystem.dat",fat);
					
					byte[] contentBytes = content.getBytes();
					
					int cont = 0;					
					 for(int i=0; i<nBlocks; i++) {
						 for (int x = 0; x < 1024; x++) {
							 if(cont >= size)
								 break;
							 data_block[x] = contentBytes[cont];
							 cont++;
						 }
					 
						 writeBlock("filesystem.dat",fatBlocks[i], data_block);
					 }
				}
			}
			else
				System.out.println("Espaço insuficiente no bloco!");
		}
		else
			System.out.println("Caminho inexistente!");
	}
	
	public static void unlink(String inPath, String name) {
	}
	
	public static void write(String inPath, String name) {
		
		
	}
	
	public static void append(String inPath, String name, int size, String content) {
		String[] path = inPath.split("/");
		DirEntry dir_entry = new DirEntry();
		int block = discoveryBlock(path);
		int fatBlock = 0;
		
		if(block != -1) { 	
			byte[] pathByte = equality(name.getBytes());
			
			for (int x = 0; x < dir_entries; ++x) {				
				dir_entry = readDirEntry(block, x);
				
				if(dir_entry.attributes == 0x01)				
					if(Arrays.equals(dir_entry.filename, pathByte)) {						
						fatBlock = dir_entry.first_block;
						break;
					}
			}
			
			Boolean exit = false;			
			while(!exit) {				
				if(fat[fatBlock] != 0x7fff && fat[fatBlock] != 0)
					fatBlock = fat[fatBlock];
				else
					exit = true;
			}
			
			int nBlocks = (int)Math.ceil(size/1024.0);
			int[] fatBlocks = new int[nBlocks];
			
			for(int i = 0; i < nBlocks; i++) {											
				fatBlocks[i] = discoveryFreeFat();
				fat[fatBlocks[i]] = 0x7fff;
			}
				
			for(int i = 0; i < fatBlocks.length -1; i++)						
				fat[fatBlocks[i]] = (short)fatBlocks[i+1];	
	
			writeFat("filesystem.dat",fat);
			
			byte[] contentBytes = content.getBytes();
			
			int cont = 0;					
			 for(int i=0; i<nBlocks; i++) {
				 for (int x = 0; x < 1024; x++) {
					 if(cont >= size)
						 break;
					 data_block[x] = contentBytes[cont];
					 cont++;
				 }
			 
				 writeBlock("filesystem.dat",fatBlocks[i], data_block);
			 }
		}
		else
			System.out.println("Caminho inexistente!");
	}

	public static void read(String inPath, String name) {
		String[] path = inPath.split("/");
		DirEntry dir_entry = new DirEntry();
		int block = discoveryBlock(path);
		int fatBlock = 0;
		
		if(block != -1) { 	
			byte[] pathByte = equality(name.getBytes());
			
			for (int x = 0; x < dir_entries; ++x) {				
				dir_entry = readDirEntry(block, x);
				
				if(dir_entry.attributes == 0x01)				
					if(Arrays.equals(dir_entry.filename, pathByte)) {						
						fatBlock = dir_entry.first_block;
						break;
					}
			}
			
			Boolean exit = false;
			StringBuilder content = new StringBuilder();
			while(!exit) {
				content.append(readBlock("filesystem.dat",fatBlock));
				if(fat[fatBlock] != 0x7fff && fat[fatBlock] != 0)
					fatBlock = fat[fatBlock];
				else
					exit = true;
			}
			
			System.out.println(content.toString());
		}
		else
			System.out.println("Caminho inexistente!");
	}
	
	public static void main(String args[]) {
		
		init();
		load();
				
		Scanner readIn = new Scanner(System.in);
		String in = ""; 
		String[] commands;		
		Boolean exit = false;
		
		while(!exit) {
			in = readIn.nextLine();	
			commands = in.split("\\s+");			
			
			switch (commands[0]) { 
				case "ls":				
					if(commands.length == 1)
						ls("");	
					else
						ls(commands[1]);					
					break;	
				case "mkdir":
					if(commands.length == 2)
						mkdir("",commands[1]);
					else if(commands.length == 3)
						mkdir(commands[1],commands[2]);
					else
						System.out.println("Comando inválido");
					break;	
				case "create":
					if(commands.length == 4)
						create("",commands[1],Integer.parseInt(commands[2]), commands[3]);
					else if(commands.length == 5)
						create(commands[1],commands[2], Integer.parseInt(commands[3]), commands[4]);
					else
						System.out.println("Comando inválido");
					break;	
				case "unlink":
					break;
				case "write":
					break;	
				case "append":
					if(commands.length == 4)
						create("",commands[1],Integer.parseInt(commands[2]), commands[3]);
					else if(commands.length == 5)
						create(commands[1],commands[2], Integer.parseInt(commands[3]), commands[4]);
					else
						System.out.println("Comando inválido");
					break;
				case "read":
					if(commands.length == 2)
						read("", commands[1]);	
					else
						read(commands[1], commands[2]);	
					break;	
					
					
					
					
					
				case "exit":
					exit = true;
					break;				
				default:					
					System.out.println("Comando inválido");
				
			}		
		}
	}
}