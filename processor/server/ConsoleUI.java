package processor.server;

import common.Settings;

import java.util.Scanner;

/**
 * Copyright (c) 2019, The University of Melbourne.
 * All rights reserved.
 * <p>
 * You are permitted to use the code for research purposes provided that the following conditions are met:
 * <p>
 * * You cannot redistribute any part of the code.
 * * You must give appropriate citations in any derived work.
 * * You cannot use any part of the code for commercial purposes.
 * * The copyright holder reserves the right to change the conditions at any time without prior notice.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * <p>
 * Created by tmuthugama on 3/15/2019
 */
public class ConsoleUI {

    private Server server;
    private Scanner sc = new Scanner(System.in);

    public ConsoleUI(Server server) {
        this.server = server;
    }

    void acceptInitialConfigFromConsole() {
        // Let user input number of workers
        System.out.println("Please specify the number of workers.");
        Settings.numWorkers = Integer.parseInt(sc.nextLine());
        while (Settings.numWorkers <= 0) {
            System.out.println("Please specify the number of workers.");
            Settings.numWorkers = Integer.parseInt(sc.nextLine());
        }
        // Kill all connected workers
        server.killConnectedWorkers();
        // Inform user next step
        System.out.println("Please launch workers now.");
    }

    void acceptSimStartCommandFromConsole() {
        System.out.println("Ready to simulate. Start (y/n)?");
        String choice = sc.nextLine();
        if (choice.equals("y") || choice.equals("Y")) {
            server.startSimulationFromLoadedScript();
        } else if (choice.equals("n") || choice.equals("N")) {
            System.out.println("Quit system.");
            server.killConnectedWorkers();
            System.exit(0);
        } else {
            System.out.println("Ready to simulate. Start (y/n)?");
        }
    }

    void acceptConsoleCommandAtSimEnd() {
        System.out.println("Simulations are completed. Exit (y/n)?");
        String choice = sc.nextLine();
        if (choice.equals("y") || choice.equals("Y")) {
            // Kill all connected workers
            System.out.println("Quit system.");
            server.killConnectedWorkers();
            System.exit(0);
        } else if (choice.equals("n") || choice.equals("N")) {
            acceptSimScriptFromConsole();
        } else {
            System.out.println("Simulations are completed. Exit (y/n)?");
        }
    }

    void acceptSimScriptFromConsole() {
        System.out.println("Please specify the simulation script path.");
        Settings.inputSimulationScript = sc.nextLine();
        while (!server.scriptLoader.loadScriptFile()) {
            System.out.println("Please specify the simulation script path.");
            Settings.inputSimulationScript = sc.nextLine();
        }
        if (server.workerMetas.size() == Settings.numWorkers) {
            acceptSimStartCommandFromConsole();
        }
    }
}
